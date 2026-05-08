package nl.lekkeratlas.worker.handler;

import com.github.davidauk.youtubescraper.model.content.Video;
import io.github.david.auk.fluid.jdbc.components.Database;
import io.github.david.auk.fluid.jdbc.components.daos.Dao;
import io.github.david.auk.fluid.jdbc.factories.DAOFactory;
import nl.lekkeratlas.shared.command.FetchVideoMetadataCommand;
import nl.lekkeratlas.shared.command.WorkCommandEnvelope;
import nl.lekkeratlas.shared.model.content.Content;
import nl.lekkeratlas.shared.model.content.ContentType;
import nl.lekkeratlas.shared.model.content.contentplatform.ContentPlatform;
import nl.lekkeratlas.shared.model.content.hostedcontent.HostedContent;
import nl.lekkeratlas.shared.model.queue.QueueJob;
import nl.lekkeratlas.shared.model.queue.QueueJobStatus;
import nl.lekkeratlas.shared.rabbit.WorkCommandUpdateProducer;
import nl.lekkeratlas.worker.exceptions.CanceledQueueJobException;
import nl.lekkeratlas.worker.exceptions.FailedQueueJobException;
import nl.lekkeratlas.worker.exceptions.QueueJobException;
import nl.lekkeratlas.worker.scraper.VideoMetadataScraper;
import nl.lekkeratlas.worker.service.QueueJobLookupService;
import nl.lekkeratlas.worker.service.UserLookupService;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

import static nl.lekkeratlas.shared.model.queue.QueueJobStatus.COMPLETED;

/**
 * Handles video imports.
 * <p>
 * Both directly added videos and videos discovered from channels end up here.
 */
@Component
public class FetchVideoMetadataCommandHandler {

        private final VideoMetadataScraper videoMetadataScraper;
        private final WorkCommandUpdateProducer workCommandUpdateProducer;
        private final UserLookupService userLookupService;
        private final QueueJobLookupService queueJobLookupService;

        public FetchVideoMetadataCommandHandler(
                VideoMetadataScraper videoMetadataScraper,
                WorkCommandUpdateProducer workCommandUpdateProducer, UserLookupService userLookupService, QueueJobLookupService queueJobLookupService
        ) {
                this.videoMetadataScraper = videoMetadataScraper;
                this.workCommandUpdateProducer = workCommandUpdateProducer;
                this.userLookupService = userLookupService;
                this.queueJobLookupService = queueJobLookupService;
        }

        // TODO Migrate to sub methods
        // TODO Remove duplicate logic/code
        public void handle(
                WorkCommandEnvelope envelope,
                FetchVideoMetadataCommand command
        ) throws QueueJobException {
                QueueJob scrapeVideoQueueJob;
                String videoId = command.videoId();

                String errorMessage = "Failed to scrape video: " + videoId;

                try (Connection connection = Database.getConnection()) {

                        // Enforce that the necessary values are present
                        userLookupService.requireExistingUser(connection, command.requestedByUserId());
                        scrapeVideoQueueJob = queueJobLookupService.requireQueueJob(connection, envelope.commandId());


                } catch (SQLException e) {
                        throw new AmqpRejectAndDontRequeueException(e);
                }

                Video videoMetadata;

                try {
                        workCommandUpdateProducer.update(
                                scrapeVideoQueueJob,
                                QueueJobStatus.RUNNING,
                                "Beginning scraping video " + command.videoId()
                        );
                        videoMetadata = videoMetadataScraper.scrape(videoId);

                        if (videoMetadata == null) {
                                throw new FailedQueueJobException(
                                        scrapeVideoQueueJob,
                                        "videoMetadata has null value",
                                        "Channel not found: " + videoId
                                );
                        }
                } catch (IOException e) {
                        throw new FailedQueueJobException(scrapeVideoQueueJob, e);
                } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();

                        throw new CanceledQueueJobException(
                                scrapeVideoQueueJob,
                                "User interrupted scraping video",
                                "Video scraping was interrupted for " + videoId
                        );
                }

                try (Connection connection = Database.getConnection()) {

                        try (Dao<ContentPlatform, UUID> contentPlatformDao = DAOFactory.createDAO(connection, ContentPlatform.class)) {
                                if (!contentPlatformDao.existsByPrimaryKey(command.contentPlatformId())) {
                                        throw new FailedQueueJobException(
                                                scrapeVideoQueueJob,
                                                "ContentPlatform ID has null value for video " + videoId,
                                                "Could not find related channel, please inform the administrator"
                                        );
                                }
                        }

                        try (
                                Dao<Content, UUID> contentDao = DAOFactory.createDAO(connection, Content.class);
                                Dao<HostedContent, UUID> hostedContentDao = DAOFactory.createDAO(connection, HostedContent.class)
                        ) {
                                Content content = new Content(
                                        UUID.randomUUID(),
                                        ContentType.OTHER,
                                        videoMetadata.getTitle(),
                                        videoMetadata.getDescription(),
                                        true,
                                        videoMetadata.getPublishedAt(),
                                        Instant.now(),
                                        Instant.now()
                                );

                                contentDao.add(content);

                                HostedContent hostedContent = new HostedContent(
                                        UUID.randomUUID(),
                                        content,

                                        // Insert an empty content platform, the ID is validated
                                        ContentPlatform.getDummyContentPlatform(command.contentPlatformId()),
                                        videoMetadata.getId()
                                );

                                hostedContentDao.add(hostedContent);
                        }

                        workCommandUpdateProducer.update(
                                connection,
                                scrapeVideoQueueJob,
                                COMPLETED,
                                "Saved video metadata for " + videoMetadata.getTitle()
                        );
                } catch (SQLException e) {
                        throw new AmqpRejectAndDontRequeueException(e);
                }
        }
}