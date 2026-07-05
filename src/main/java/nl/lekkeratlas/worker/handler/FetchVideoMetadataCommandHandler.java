package nl.lekkeratlas.worker.handler;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.stereotype.Component;

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
                        WorkCommandUpdateProducer workCommandUpdateProducer, UserLookupService userLookupService,
                        QueueJobLookupService queueJobLookupService) {
                this.videoMetadataScraper = videoMetadataScraper;
                this.workCommandUpdateProducer = workCommandUpdateProducer;
                this.userLookupService = userLookupService;
                this.queueJobLookupService = queueJobLookupService;
        }

        /**
         * TODO fill
         * 
         * @param envelope
         * @param command
         * @throws QueueJobException
         */
        public void handle(
                        WorkCommandEnvelope envelope,
                        FetchVideoMetadataCommand command) throws QueueJobException {
                QueueJob scrapeVideoQueueJob = validateAndLoadScrapeVideoQueueJob(envelope, command);
                Video videoMetadata = scrapeVideoMetadata(scrapeVideoQueueJob, command.videoId());

                saveVideoMetadata(command, scrapeVideoQueueJob, videoMetadata);
        }

        /**
         * @param envelope The incomming queued request
         * @param command  The command that
         * @return A validated QueueJob object that is from the same user
         *         that made the request
         */
        private QueueJob validateAndLoadScrapeVideoQueueJob(
                        WorkCommandEnvelope envelope,
                        FetchVideoMetadataCommand command) {
                try (Connection connection = Database.getConnection()) {
                        userLookupService.requireExistingUser(connection, command.requestedByUserId());

                        return queueJobLookupService.requireQueueJob(connection, envelope.commandId());
                } catch (SQLException e) {
                        throw new AmqpRejectAndDontRequeueException(e);
                }
        }

        private Video scrapeVideoMetadata(
                        QueueJob scrapeVideoQueueJob,
                        String videoId) throws QueueJobException {
                try {
                        workCommandUpdateProducer.update(
                                        scrapeVideoQueueJob,
                                        QueueJobStatus.RUNNING,
                                        "Beginning scraping video " + videoId);

                        Video videoMetadata = videoMetadataScraper.scrape(videoId);

                        requireValidVideoMetadata(scrapeVideoQueueJob, videoId,
                                        videoMetadata);

                        return videoMetadata;
                } catch (IOException e) {
                        throw new FailedQueueJobException(scrapeVideoQueueJob, e);
                } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();

                        throw new CanceledQueueJobException(
                                        scrapeVideoQueueJob,
                                        "User interrupted scraping video",
                                        "Video scraping was interrupted for " + videoId);
                }
        }

        private void requireValidVideoMetadata(
                        QueueJob scrapeVideoQueueJob,
                        String videoId,
                        Video videoMetadata) throws FailedQueueJobException {
                if (videoMetadata == null) {
                        throw new FailedQueueJobException(
                                        scrapeVideoQueueJob,
                                        "videoMetadata has null value",
                                        "Video not found: " + videoId);
                }

                if (videoMetadata.getTitle() == null
                                || videoMetadata.getTitle().isEmpty()
                                || videoMetadata.getPublishedAt() == null) {
                        throw new FailedQueueJobException(
                                        scrapeVideoQueueJob,
                                        "Scraped video data is missing a required value",
                                        "Missing essential data from the scraped video data");
                }
        }

        private void saveVideoMetadata(
                        FetchVideoMetadataCommand command,
                        QueueJob scrapeVideoQueueJob,
                        Video videoMetadata) throws QueueJobException {
                try (Connection connection = Database.getConnection()) {
                        requireExistingContentPlatform(connection, command, scrapeVideoQueueJob);

                        saveContentAndHostedContent(connection, command, videoMetadata);

                        workCommandUpdateProducer.update(
                                        connection,
                                        scrapeVideoQueueJob,
                                        QueueJobStatus.COMPLETED,
                                        "Saved video metadata for " + videoMetadata.getTitle());
                } catch (SQLException e) {
                        throw new AmqpRejectAndDontRequeueException(e);
                }
        }

        private void requireExistingContentPlatform(
                        Connection connection,
                        FetchVideoMetadataCommand command,
                        QueueJob scrapeVideoQueueJob) throws FailedQueueJobException {
                try (Dao<ContentPlatform, UUID> contentPlatformDao = DAOFactory.createDAO(
                                connection,
                                ContentPlatform.class)) {
                        if (!contentPlatformDao.existsByPrimaryKey(command.contentPlatformId())) {
                                throw new FailedQueueJobException(
                                                scrapeVideoQueueJob,
                                                "ContentPlatform ID has null value for video " + command.videoId(),
                                                "Could not find related content platform, please inform the administrator");
                        }
                }
        }

        private void saveContentAndHostedContent(
                        Connection connection,
                        FetchVideoMetadataCommand command,
                        Video videoMetadata) {
                try (
                                Dao<Content, UUID> contentDao = DAOFactory.createDAO(connection, Content.class);
                                Dao<HostedContent, UUID> hostedContentDao = DAOFactory.createDAO(connection,
                                                HostedContent.class)) {
                        Content content = createContent(videoMetadata);

                        contentDao.add(content);

                        HostedContent hostedContent = createHostedContent(command, videoMetadata, content);

                        hostedContentDao.add(hostedContent);
                }
        }

        private Content createContent(Video videoMetadata) {
                Instant now = Instant.now();

                return new Content(
                                UUID.randomUUID(),
                                ContentType.OTHER,
                                videoMetadata.getTitle(),
                                videoMetadata.getDescription(),
                                true,
                                videoMetadata.getPublishedAt(),
                                now,
                                now);
        }

        private HostedContent createHostedContent(
                        FetchVideoMetadataCommand command,
                        Video videoMetadata,
                        Content content) {
                return new HostedContent(
                                UUID.randomUUID(),
                                content,

                                // Insert an empty content platform, the ID is validated before saving
                                ContentPlatform.getDummyContentPlatform(command.contentPlatformId()),
                                videoMetadata.getId());
        }
}
