package nl.lekkeratlas.worker.handler;

import com.github.davidauk.youtubescraper.model.Channel;
import com.github.davidauk.youtubescraper.model.ChannelOverviewResponse;
import com.github.davidauk.youtubescraper.model.content.PartialVideo;
import io.github.david.auk.fluid.jdbc.components.Database;
import io.github.david.auk.fluid.jdbc.components.daos.Dao;
import io.github.david.auk.fluid.jdbc.components.daos.DaoTransactional;
import io.github.david.auk.fluid.jdbc.components.daos.QueryBuilder;
import io.github.david.auk.fluid.jdbc.factories.DAOFactory;
import nl.lekkeratlas.shared.command.AddVideoSource;
import nl.lekkeratlas.shared.command.FetchPlatformContentCommand;
import nl.lekkeratlas.shared.command.FetchVideoMetadataCommand;
import nl.lekkeratlas.shared.command.WorkCommandEnvelope;
import nl.lekkeratlas.shared.model.content.Content;
import nl.lekkeratlas.shared.model.content.contentplatform.*;
import nl.lekkeratlas.shared.model.content.hostedcontent.HostedContent;
import nl.lekkeratlas.shared.model.queue.QueueJob;
import nl.lekkeratlas.shared.model.queue.QueueJobStatus;
import nl.lekkeratlas.shared.model.queue.QueueJobType;
import nl.lekkeratlas.shared.model.user.User;
import nl.lekkeratlas.shared.rabbit.WorkCommandProducer;
import nl.lekkeratlas.shared.rabbit.WorkCommandUpdateProducer;
import nl.lekkeratlas.worker.exceptions.CanceledQueueJobException;
import nl.lekkeratlas.worker.exceptions.FailedQueueJobException;
import nl.lekkeratlas.worker.exceptions.QueueJobException;
import nl.lekkeratlas.worker.scraper.ChannelScraper;
import nl.lekkeratlas.worker.service.QueueJobLookupService;
import nl.lekkeratlas.worker.service.UserLookupService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.github.david.auk.fluid.jdbc.components.daos.querying.operator.SingleValueOperator.EQUALS;

/**
 * Handles channel imports.
 * <p>
 * This handler should only discover videos and enqueue FetchVideoMetadataCommand messages.
 * The actual metadata scraping belongs in FetchVideoMetadataCommandHandler.
 */
@Component
public class FetchPlatformContentCommandHandler {

        private final ChannelScraper channelScraper;
        private final WorkCommandProducer workCommandProducer;
        private final WorkCommandUpdateProducer workCommandUpdateProducer;
        private final UserLookupService userLookupService;
        private final QueueJobLookupService queueJobLookupService;

        public FetchPlatformContentCommandHandler(
                ChannelScraper channelScraper,
                WorkCommandProducer workCommandProducer,
                WorkCommandUpdateProducer workCommandUpdateProducer, UserLookupService userLookupService, QueueJobLookupService queueJobLookupService
        ) {
                this.channelScraper = channelScraper;
                this.workCommandProducer = workCommandProducer;
                this.workCommandUpdateProducer = workCommandUpdateProducer;
                this.userLookupService = userLookupService;
                this.queueJobLookupService = queueJobLookupService;
        }

        public void handle(
                WorkCommandEnvelope envelope,
                FetchPlatformContentCommand command
        ) throws QueueJobException, SQLException {
                User user;
                QueueJob scrapeChannelQueueJob;
                try (Connection connection = Database.getConnection()) {

                        // Enforce that the necessary values are present
                        user = userLookupService.requireExistingUser(connection, command.requestedByUserId());
                        scrapeChannelQueueJob = queueJobLookupService.requireCleanQueueJob(connection, envelope.commandId());
                }

                String channelId = command.channelId();

                if (channelId == null || channelId.isEmpty()) {
                        throw new FailedQueueJobException(
                                scrapeChannelQueueJob,
                                "User gave a empty request",
                                "Channel ID cannot be empty"
                        );
                }

                ChannelOverviewResponse channelOverviewResponse;

                QueueJobStatus status;
                String message;

                try {
                        workCommandUpdateProducer.update(
                                scrapeChannelQueueJob,
                                QueueJobStatus.RUNNING,
                                "Beginning scraping channel " + command.channelId()
                        );
                        channelOverviewResponse = channelScraper.findVideoIds(channelId);
                        if (channelOverviewResponse == null) {
                                throw new FailedQueueJobException(
                                        scrapeChannelQueueJob,
                                        "channelOverviewResponse has null value",
                                        "Channel not found: " + channelId
                                );
                        }
                        status = QueueJobStatus.RUNNING;
                        message = "Retrieved " + channelOverviewResponse.videos().size() + " videos for: " +
                                channelOverviewResponse.channel().title();
                } catch (IOException e) {
                        throw new FailedQueueJobException(scrapeChannelQueueJob, e);
                } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();

                        throw new CanceledQueueJobException(
                                scrapeChannelQueueJob,
                                "User interrupted scraping channel",
                                "Channel scraping was interrupted for channel " + channelId
                        );
                }

                try (Connection connection = Database.getConnection()) {
                        workCommandUpdateProducer.update(
                                connection,
                                scrapeChannelQueueJob,
                                status,
                                message
                        );

                        YoutubeChannel youtubeChannel = addYoutubeChannel(channelOverviewResponse.channel(), user);
                        syncVideos(
                                connection,
                                channelOverviewResponse,
                                youtubeChannel,
                                user,
                                scrapeChannelQueueJob
                        );

                        workCommandUpdateProducer.update(
                                connection,
                                scrapeChannelQueueJob,
                                QueueJobStatus.COMPLETED,
                                "Finished scraping channel " + channelId
                        );
                } catch (SQLException e) {
                        throw new RuntimeException(e);
                }
        }

        private void syncVideos(
                Connection connection,
                ChannelOverviewResponse response,
                YoutubeChannel youtubeChannel,
                User user,
                QueueJob parentQueueJob
        ) throws SQLException {
                List<HostedContent> existingVideos = findExistingVideos(connection, youtubeChannel);

                Map<String, HostedContent> existingByExternalId = existingVideos.stream()
                        .collect(Collectors.toMap(HostedContent::externalContentId, video -> video));

                List<PartialVideo> newVideos = new ArrayList<>();

                for (PartialVideo partialVideo : response.videos()) {
                        HostedContent existingVideo = existingByExternalId.get(partialVideo.id());

                        if (existingVideo == null) {
                                newVideos.add(partialVideo);
                                continue;
                        }

                        updateExistingVideo(connection, existingVideo, partialVideo);
                }

                addVideos(connection, newVideos, youtubeChannel, user, parentQueueJob);
        }

        private void addVideos(Connection connection, List<PartialVideo> videos, ContentPlatform contentPlatform,
                               User requestedBy, QueueJob parentQueueJob) {
                try (Dao<QueueJob, UUID> queueJobDao = DAOFactory.createDAO(connection, QueueJob.class)) {
                        for (PartialVideo video : videos) {

                                workCommandProducer.publish(
                                        QueueJobType.FETCH_VIDEO_METADATA,
                                        new FetchVideoMetadataCommand(
                                                video.id(),
                                                requestedBy.getId(),
                                                contentPlatform.getId(),
                                                AddVideoSource.DISCOVERED_FROM_CHANNEL
                                        ),
                                        parentQueueJob,
                                        queueJobDao
                                );
                        }
                }
        }

        private List<HostedContent> findExistingVideos(Connection connection, YoutubeChannel youtubeChannel) {
                try (Dao<HostedContent, UUID> hostedContentDao = DAOFactory.createDAO(connection, HostedContent.class)) {
                        return new QueryBuilder<>(hostedContentDao)
                                .where(
                                        HostedContent.class.getDeclaredField("contentPlatform"),
                                        EQUALS,
                                        youtubeChannel.getId()
                                ).get();
                } catch (NoSuchFieldException e) {
                        throw new RuntimeException(e);
                }
        }

        private void updateExistingVideo(
                Connection connection,
                HostedContent existingVideo,
                PartialVideo partialVideo
        ) {
                Content content = existingVideo.content();

                content.setTitle(partialVideo.title());

                try (Dao<Content, UUID> contentDao = DAOFactory.createDAO(connection, Content.class)) {
                        contentDao.update(content);
                }
        }


        private YoutubeChannel addYoutubeChannel(Channel channel, User addedBy) throws SQLException {

                String channelId = channel.channelId().channelId(); //  TODO Improve this naming in the scraper project

                try (Connection transactionalConnection = Database.getConnection()) {
                        transactionalConnection.setAutoCommit(false);

                        // Add to ContentPlatform table
                        try (
                                DaoTransactional<ContentPlatform, UUID> contentPlatformDao = DAOFactory.createTransactionalDAO(
                                        transactionalConnection,
                                        ContentPlatform.class
                                );
                                DaoTransactional<ContentVideoPlatform, UUID> contentVideoPlatformDao = DAOFactory.createTransactionalDAO(
                                        transactionalConnection,
                                        ContentVideoPlatform.class
                                );
                                DaoTransactional<YoutubeChannel, UUID> youtubeChannelDao = DAOFactory.createTransactionalDAO(
                                        transactionalConnection,
                                        YoutubeChannel.class
                                )

                        ) {

                                YoutubeChannel existingChannel = getYoutubeChannel(youtubeChannelDao, channelId);

                                if (existingChannel != null) {
                                        return existingChannel;
                                }

                                ContentPlatform contentPlatform = new ContentPlatform(
                                        UUID.randomUUID(),
                                        ContentPlatformKind.VIDEO,
                                        channel.title(),
                                        false,
                                        addedBy,
                                        Instant.now()
                                );

                                ContentVideoPlatform contentVideoPlatform = new ContentVideoPlatform(
                                        contentPlatform,
                                        SourceKind.YOUTUBE_CHANNEL
                                );

                                YoutubeChannel youtubeChannel = new YoutubeChannel(
                                        contentVideoPlatform,
                                        channelId
                                );

                                contentPlatformDao.add(contentPlatform);
                                contentVideoPlatformDao.add(contentVideoPlatform);
                                youtubeChannelDao.add(youtubeChannel);
                                transactionalConnection.commit();
                                existingChannel = youtubeChannel;

                                return existingChannel;
                        }
                }
        }

        private YoutubeChannel getYoutubeChannel(Dao<YoutubeChannel, UUID> youtubeChannelDao, String channelId) {
                try {
                        return new QueryBuilder<>(youtubeChannelDao)
                                .where(YoutubeChannel.class.getDeclaredField("youtubeChannelId"),
                                        EQUALS, channelId)
                                .getUnique();
                } catch (NoSuchFieldException e) {
                        throw new RuntimeException(e);
                }
        }

}