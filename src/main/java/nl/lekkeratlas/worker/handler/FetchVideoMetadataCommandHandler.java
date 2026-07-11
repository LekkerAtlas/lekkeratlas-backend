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
import nl.lekkeratlas.worker.exceptions.FailedQueueJobException;
import nl.lekkeratlas.worker.exceptions.QueueJobException;
import nl.lekkeratlas.worker.execution.QueueJobCancellationToken;
import nl.lekkeratlas.worker.scraper.VideoMetadataScraper;
import nl.lekkeratlas.worker.service.QueueJobLookupService;
import nl.lekkeratlas.worker.service.UserLookupService;

/**
 * Handles video imports.
 *
 * <p>
 * Both directly added videos and videos discovered from channels end up
 * here.
 */
@Component
public class FetchVideoMetadataCommandHandler {

        private final VideoMetadataScraper videoMetadataScraper;
        private final WorkCommandUpdateProducer workCommandUpdateProducer;
        private final UserLookupService userLookupService;
        private final QueueJobLookupService queueJobLookupService;

        public FetchVideoMetadataCommandHandler(
                        VideoMetadataScraper videoMetadataScraper,
                        WorkCommandUpdateProducer workCommandUpdateProducer,
                        UserLookupService userLookupService,
                        QueueJobLookupService queueJobLookupService) {

                this.videoMetadataScraper = videoMetadataScraper;
                this.workCommandUpdateProducer = workCommandUpdateProducer;
                this.userLookupService = userLookupService;
                this.queueJobLookupService = queueJobLookupService;
        }

        /**
         * Handles a video-metadata command.
         *
         * @param envelope     the incoming queued request
         * @param command      the video-metadata command
         * @param cancellation cancellation state for this queue-job execution
         * @throws QueueJobException when the queue job fails or is canceled
         */
        public void handle(
                        WorkCommandEnvelope envelope,
                        FetchVideoMetadataCommand command,
                        QueueJobCancellationToken cancellation)
                        throws QueueJobException {

                QueueJob scrapeVideoQueueJob = validateAndLoadScrapeVideoQueueJob(
                                envelope,
                                command);

                /*
                 * Handle jobs that were already canceled before this handler
                 * started. This closes the small gap before the cancellation
                 * monitor performs its first poll.
                 */
                if (scrapeVideoQueueJob.isCanceled()) {
                        throw cancellation.canceledException(
                                        "Video metadata job was canceled before execution");
                }

                cancellation.checkpoint("Video metadata job was canceled before scraping started");

                Video videoMetadata = scrapeVideoMetadata(
                                scrapeVideoQueueJob,
                                command.videoId(),
                                cancellation);

                cancellation.checkpoint("Video metadata job was canceled after scraping");

                saveVideoMetadata(
                                command,
                                scrapeVideoQueueJob,
                                videoMetadata,
                                cancellation);
        }

        /**
         * Loads and validates the user and queue job associated with the
         * command.
         *
         * @param envelope the incoming queued request
         * @param command  the command being handled
         * @return the related queue job
         */
        private QueueJob validateAndLoadScrapeVideoQueueJob(
                        WorkCommandEnvelope envelope,
                        FetchVideoMetadataCommand command) {

                try (Connection connection = Database.getConnection()) {
                        userLookupService.requireExistingUser(
                                        connection,
                                        command.requestedByUserId());

                        return queueJobLookupService.requireQueueJob(
                                        connection,
                                        envelope.commandId());
                } catch (SQLException exception) {
                        throw new AmqpRejectAndDontRequeueException(
                                        exception);
                }
        }

        private Video scrapeVideoMetadata(
                        QueueJob scrapeVideoQueueJob,
                        String videoId,
                        QueueJobCancellationToken cancellation)
                        throws QueueJobException {

                cancellation.checkpoint("Video metadata job was canceled before starting the scraper");

                try {
                        workCommandUpdateProducer.update(
                                        scrapeVideoQueueJob,
                                        QueueJobStatus.RUNNING,
                                        "Beginning scraping video " + videoId);

                        Video videoMetadata = videoMetadataScraper.scrape(videoId);

                        cancellation.checkpoint("Video metadata job was canceled while scraping video "
                                        + videoId);

                        requireValidVideoMetadata(
                                        scrapeVideoQueueJob,
                                        videoId,
                                        videoMetadata);

                        return videoMetadata;
                } catch (InterruptedException exception) {
                        /*
                         * The cancellation monitor marks the token before
                         * interrupting the dedicated queue-job thread.
                         */
                        if (cancellation.isCancellationRequested()) {
                                throw cancellation.canceledException(
                                                "Video scraping was canceled for "
                                                                + videoId);
                        }

                        /*
                         * This interruption did not originate from a user
                         * cancellation. Preserve it and report the execution as
                         * failed.
                         */
                        Thread.currentThread().interrupt();

                        throw new FailedQueueJobException(
                                        scrapeVideoQueueJob,
                                        exception);
                } catch (IOException exception) {
                        throw new FailedQueueJobException(
                                        scrapeVideoQueueJob,
                                        exception);
                }
        }

        private void requireValidVideoMetadata(
                        QueueJob scrapeVideoQueueJob,
                        String videoId,
                        Video videoMetadata)
                        throws FailedQueueJobException {

                if (videoMetadata == null) {
                        throw new FailedQueueJobException(
                                        scrapeVideoQueueJob,
                                        "videoMetadata has null value",
                                        "Video not found: " + videoId);
                }

                if (videoMetadata.getTitle() == null
                                || videoMetadata.getTitle().isBlank()
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
                        Video videoMetadata,
                        QueueJobCancellationToken cancellation)
                        throws QueueJobException {

                try (Connection connection = Database.getConnection()) {
                        cancellation.checkpoint(
                                        "Video metadata job was canceled before validating the content platform");

                        requireExistingContentPlatform(
                                        connection,
                                        command,
                                        scrapeVideoQueueJob);

                        cancellation.checkpoint("Video metadata job was canceled before saving the video");

                        saveContentAndHostedContent(
                                        connection,
                                        command,
                                        videoMetadata);

                        /*
                         * Prevent a cancellation near the end of the operation
                         * from being overwritten by COMPLETED.
                         */
                        cancellation.checkpoint("Video metadata job was canceled before completion");

                        workCommandUpdateProducer.update(
                                        connection,
                                        scrapeVideoQueueJob,
                                        QueueJobStatus.COMPLETED,
                                        "Saved video metadata for "
                                                        + videoMetadata.getTitle());
                } catch (SQLException exception) {
                        throw new AmqpRejectAndDontRequeueException(
                                        exception);
                }
        }

        private void requireExistingContentPlatform(
                        Connection connection,
                        FetchVideoMetadataCommand command,
                        QueueJob scrapeVideoQueueJob)
                        throws FailedQueueJobException {

                try (Dao<ContentPlatform, UUID> contentPlatformDao = DAOFactory.createDAO(
                                connection,
                                ContentPlatform.class)) {

                        if (!contentPlatformDao.existsByPrimaryKey(
                                        command.contentPlatformId())) {

                                throw new FailedQueueJobException(
                                                scrapeVideoQueueJob,
                                                "ContentPlatform ID has null value for video "
                                                                + command.videoId(),
                                                "Could not find related content platform, "
                                                                + "please inform the administrator");
                        }
                }
        }

        private void saveContentAndHostedContent(
                        Connection connection,
                        FetchVideoMetadataCommand command,
                        Video videoMetadata) {

                try (
                                Dao<Content, UUID> contentDao = DAOFactory.createDAO(
                                                connection,
                                                Content.class);

                                Dao<HostedContent, UUID> hostedContentDao = DAOFactory.createDAO(
                                                connection,
                                                HostedContent.class)) {

                        Content content = createContent(videoMetadata);

                        contentDao.add(content);

                        HostedContent hostedContent = createHostedContent(
                                        command,
                                        videoMetadata,
                                        content);

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

                                /*
                                 * Insert a placeholder content platform. Its ID
                                 * was validated before saving.
                                 */
                                ContentPlatform.getDummyContentPlatform(
                                                command.contentPlatformId()),
                                videoMetadata.getId());
        }
}
