package nl.lekkeratlas.worker.handler;

import static io.github.david.auk.fluid.jdbc.components.daos.querying.operator.SingleValueOperator.EQUALS;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

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
import nl.lekkeratlas.shared.model.content.contentplatform.ContentPlatform;
import nl.lekkeratlas.shared.model.content.contentplatform.ContentPlatformKind;
import nl.lekkeratlas.shared.model.content.contentplatform.ContentVideoPlatform;
import nl.lekkeratlas.shared.model.content.contentplatform.SourceKind;
import nl.lekkeratlas.shared.model.content.contentplatform.YoutubeChannel;
import nl.lekkeratlas.shared.model.content.hostedcontent.HostedContent;
import nl.lekkeratlas.shared.model.queue.QueueJob;
import nl.lekkeratlas.shared.model.queue.QueueJobStatus;
import nl.lekkeratlas.shared.model.queue.QueueJobType;
import nl.lekkeratlas.shared.model.user.User;
import nl.lekkeratlas.shared.rabbit.WorkCommandProducer;
import nl.lekkeratlas.shared.rabbit.WorkCommandUpdateProducer;
import nl.lekkeratlas.worker.exceptions.FailedQueueJobException;
import nl.lekkeratlas.worker.exceptions.QueueJobException;
import nl.lekkeratlas.worker.execution.QueueJobCancellationToken;
import nl.lekkeratlas.worker.scraper.ChannelScraper;
import nl.lekkeratlas.worker.service.QueueJobLookupService;
import nl.lekkeratlas.worker.service.UserLookupService;

/**
 * Handles channel imports.
 *
 * <p>
 * This handler discovers videos and enqueues
 * {@link FetchVideoMetadataCommand} messages. Metadata scraping is handled by
 * {@link FetchVideoMetadataCommandHandler}.
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
                        WorkCommandUpdateProducer workCommandUpdateProducer,
                        UserLookupService userLookupService,
                        QueueJobLookupService queueJobLookupService) {

                this.channelScraper = channelScraper;
                this.workCommandProducer = workCommandProducer;
                this.workCommandUpdateProducer = workCommandUpdateProducer;
                this.userLookupService = userLookupService;
                this.queueJobLookupService = queueJobLookupService;
        }

        public void handle(
                        WorkCommandEnvelope envelope,
                        FetchPlatformContentCommand command,
                        QueueJobCancellationToken cancellation)
                        throws QueueJobException, SQLException, NoSuchFieldException {

                User user;
                QueueJob scrapeChannelQueueJob;

                try (Connection connection = Database.getConnection()) {
                        user = userLookupService.requireExistingUser(
                                        connection,
                                        command.requestedByUserId());

                        scrapeChannelQueueJob = queueJobLookupService.requireCleanQueueJob(
                                        connection,
                                        envelope.commandId());
                }

                String channelId = command.channelId();

                if (channelId == null || channelId.isBlank()) {
                        throw new FailedQueueJobException(
                                        scrapeChannelQueueJob,
                                        "User gave an empty request",
                                        "Channel ID cannot be empty");
                }

                cancellation.checkpoint("Channel import was canceled before scraping started");

                workCommandUpdateProducer.update(
                                scrapeChannelQueueJob,
                                QueueJobStatus.RUNNING,
                                "Beginning scraping channel " + channelId);

                ChannelOverviewResponse channelOverviewResponse = fetchChannelOverview(
                                channelId,
                                scrapeChannelQueueJob,
                                cancellation);

                cancellation.checkpoint("Channel import was canceled after scraping the channel");

                if (channelOverviewResponse == null) {
                        throw new FailedQueueJobException(
                                        scrapeChannelQueueJob,
                                        "channelOverviewResponse has null value",
                                        "Channel not found: " + channelId);
                }

                try (Connection connection = Database.getConnection()) {
                        workCommandUpdateProducer.update(
                                        connection,
                                        scrapeChannelQueueJob,
                                        QueueJobStatus.RUNNING,
                                        "Retrieved "
                                                        + channelOverviewResponse.videos().size()
                                                        + " videos for: "
                                                        + channelOverviewResponse
                                                                        .channel()
                                                                        .title());

                        cancellation.checkpoint("Channel import was canceled before saving the channel");

                        YoutubeChannel youtubeChannel = addYoutubeChannel(
                                        channelOverviewResponse.channel(),
                                        user);

                        cancellation.checkpoint("Channel import was canceled before synchronizing videos");

                        syncVideos(
                                        connection,
                                        channelOverviewResponse,
                                        youtubeChannel,
                                        user,
                                        scrapeChannelQueueJob,
                                        cancellation);

                        workCommandUpdateProducer.update(connection, scrapeChannelQueueJob, QueueJobStatus.COMPLETED,
                                        "Finished scraping channel " + channelId);
                }
        }

        private ChannelOverviewResponse fetchChannelOverview(
                        String channelId,
                        QueueJob queueJob,
                        QueueJobCancellationToken cancellation)
                        throws QueueJobException {

                try {
                        return channelScraper.findVideoIds(channelId);
                } catch (InterruptedException exception) {
                        /*
                         * An expected cancellation interrupt is translated into
                         * a domain-level CANCELED exception.
                         */
                        if (cancellation.isCancellationRequested()) {
                                throw cancellation.canceledException(
                                                "Channel scraping was canceled for channel "
                                                                + channelId);
                        }

                        /*
                         * This interruption was not triggered by queue-job
                         * cancellation, so preserve it and report a failure.
                         */
                        Thread.currentThread().interrupt();

                        throw new FailedQueueJobException(
                                        queueJob,
                                        exception);
                } catch (IOException exception) {
                        throw new FailedQueueJobException(
                                        queueJob,
                                        exception);
                }
        }

        private void syncVideos(
                        Connection connection,
                        ChannelOverviewResponse response,
                        YoutubeChannel youtubeChannel,
                        User user,
                        QueueJob parentQueueJob,
                        QueueJobCancellationToken cancellation)
                        throws NoSuchFieldException, QueueJobException {

                List<HostedContent> existingVideos = findExistingVideos(
                                connection,
                                youtubeChannel);

                Map<String, HostedContent> existingByExternalId = existingVideos.stream()
                                .collect(Collectors.toMap(
                                                HostedContent::externalContentId,
                                                video -> video));

                List<PartialVideo> newVideos = new ArrayList<>();

                for (PartialVideo partialVideo : response.videos()) {
                        cancellation.checkpoint("Channel import was canceled while synchronizing videos");

                        HostedContent existingVideo = existingByExternalId.get(
                                        partialVideo.id());

                        if (existingVideo == null) {
                                newVideos.add(partialVideo);
                                continue;
                        }

                        updateExistingVideo(
                                        connection,
                                        existingVideo,
                                        partialVideo);
                }

                addVideos(
                                connection,
                                newVideos,
                                youtubeChannel,
                                user,
                                parentQueueJob,
                                cancellation);
        }

        private void addVideos(
                        Connection connection,
                        List<PartialVideo> videos,
                        ContentPlatform contentPlatform,
                        User requestedBy,
                        QueueJob parentQueueJob,
                        QueueJobCancellationToken cancellation)
                        throws QueueJobException {

                try (Dao<QueueJob, UUID> queueJobDao = DAOFactory.createDAO(
                                connection,
                                QueueJob.class)) {

                        for (PartialVideo video : videos) {
                                cancellation.checkpoint("Channel import was canceled while creating metadata jobs");

                                workCommandProducer.publish(
                                                QueueJobType.FETCH_VIDEO_METADATA,
                                                new FetchVideoMetadataCommand(
                                                                video.id(),
                                                                requestedBy.getId(),
                                                                contentPlatform.getId(),
                                                                AddVideoSource.DISCOVERED_FROM_CHANNEL),
                                                parentQueueJob,
                                                queueJobDao);
                        }
                }
        }

        private List<HostedContent> findExistingVideos(
                        Connection connection,
                        YoutubeChannel youtubeChannel)
                        throws NoSuchFieldException {

                try (Dao<HostedContent, UUID> hostedContentDao = DAOFactory.createDAO(
                                connection,
                                HostedContent.class)) {

                        return new QueryBuilder<>(hostedContentDao)
                                        .where(
                                                        HostedContent.class.getDeclaredField(
                                                                        "contentPlatform"),
                                                        EQUALS,
                                                        youtubeChannel.getId())
                                        .get();
                }
        }

        private void updateExistingVideo(
                        Connection connection,
                        HostedContent existingVideo,
                        PartialVideo partialVideo) {

                Content content = existingVideo.content();
                content.setTitle(partialVideo.title());

                try (Dao<Content, UUID> contentDao = DAOFactory.createDAO(
                                connection,
                                Content.class)) {

                        contentDao.update(content);
                }
        }

        private YoutubeChannel addYoutubeChannel(
                        Channel channel,
                        User addedBy)
                        throws SQLException, NoSuchFieldException {

                // TODO: Improve this naming in the scraper project.
                String channelId = channel.channelId().channelId();

                try (Connection transactionalConnection = Database.getConnection()) {

                        transactionalConnection.setAutoCommit(false);

                        try (
                                        DaoTransactional<ContentPlatform, UUID> contentPlatformDao = DAOFactory
                                                        .createTransactionalDAO(
                                                                        transactionalConnection,
                                                                        ContentPlatform.class);

                                        DaoTransactional<ContentVideoPlatform, UUID> contentVideoPlatformDao = DAOFactory
                                                        .createTransactionalDAO(
                                                                        transactionalConnection,
                                                                        ContentVideoPlatform.class);

                                        DaoTransactional<YoutubeChannel, UUID> youtubeChannelDao = DAOFactory
                                                        .createTransactionalDAO(
                                                                        transactionalConnection,
                                                                        YoutubeChannel.class)) {

                                YoutubeChannel existingChannel = getYoutubeChannel(
                                                youtubeChannelDao,
                                                channelId);

                                if (existingChannel != null) {
                                        return existingChannel;
                                }

                                ContentPlatform contentPlatform = new ContentPlatform(
                                                UUID.randomUUID(),
                                                ContentPlatformKind.VIDEO,
                                                channel.title(),
                                                false,
                                                addedBy,
                                                Instant.now());

                                ContentVideoPlatform contentVideoPlatform = new ContentVideoPlatform(
                                                contentPlatform,
                                                SourceKind.YOUTUBE_CHANNEL);

                                YoutubeChannel youtubeChannel = new YoutubeChannel(
                                                contentVideoPlatform,
                                                channelId);

                                contentPlatformDao.add(contentPlatform);
                                contentVideoPlatformDao.add(
                                                contentVideoPlatform);
                                youtubeChannelDao.add(youtubeChannel);

                                transactionalConnection.commit();

                                return youtubeChannel;
                        } catch (SQLException | RuntimeException exception) {
                                transactionalConnection.rollback();
                                throw exception;
                        }
                }
        }

        private YoutubeChannel getYoutubeChannel(
                        Dao<YoutubeChannel, UUID> youtubeChannelDao,
                        String channelId)
                        throws NoSuchFieldException {

                return new QueryBuilder<>(youtubeChannelDao)
                                .where(
                                                YoutubeChannel.class.getDeclaredField(
                                                                "youtubeChannelId"),
                                                EQUALS,
                                                channelId)
                                .getUnique();
        }
}
