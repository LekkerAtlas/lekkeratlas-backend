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
import io.github.david.auk.fluid.jdbc.components.daos.DaoTransactional;
import io.github.david.auk.fluid.jdbc.factories.DAOFactory;

import nl.lekkeratlas.shared.command.FetchVideoMetadataCommand;
import nl.lekkeratlas.shared.command.WorkCommandEnvelope;
import nl.lekkeratlas.shared.model.content.Content;
import nl.lekkeratlas.shared.model.content.ContentType;
import nl.lekkeratlas.shared.model.content.creator.Creator;
import nl.lekkeratlas.shared.model.content.creator.CreatorAccount;
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
 * Both directly added videos and videos discovered from creator accounts end up
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

        public void handle(
                        WorkCommandEnvelope envelope,
                        FetchVideoMetadataCommand command,
                        QueueJobCancellationToken cancellation)
                        throws QueueJobException {

                QueueJob scrapeVideoQueueJob = validateAndLoadScrapeVideoQueueJob(
                                envelope,
                                command);

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

                        cancellation.checkpoint(
                                        "Video metadata job was canceled while scraping video "
                                                        + videoId);

                        requireValidVideoMetadata(
                                        scrapeVideoQueueJob,
                                        videoId,
                                        videoMetadata);

                        return videoMetadata;
                } catch (InterruptedException exception) {
                        if (cancellation.isCancellationRequested()) {
                                throw cancellation.canceledException(
                                                "Video scraping was canceled for "
                                                                + videoId);
                        }

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
                                        "Video metadata job was canceled before validating the creator account");

                        CreatorAccount creatorAccount = requireExistingCreatorAccount(
                                        connection,
                                        command,
                                        scrapeVideoQueueJob);

                        cancellation.checkpoint("Video metadata job was canceled before saving the video");

                        saveContentAndHostedContent(
                                        connection,
                                        creatorAccount,
                                        videoMetadata);

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

        private CreatorAccount requireExistingCreatorAccount(
                        Connection connection,
                        FetchVideoMetadataCommand command,
                        QueueJob scrapeVideoQueueJob)
                        throws FailedQueueJobException {

                try (Dao<CreatorAccount, UUID> creatorAccountDao = DAOFactory.createDAO(
                                connection,
                                CreatorAccount.class)) {

                        CreatorAccount creatorAccount = creatorAccountDao.get(
                                        command.creatorAccountId());

                        if (creatorAccount == null || creatorAccount.getCreator() == null) {
                                throw new FailedQueueJobException(
                                                scrapeVideoQueueJob,
                                                "CreatorAccount ID could not be resolved for video "
                                                                + command.videoId(),
                                                "Could not find the related creator account, "
                                                                + "please inform the administrator");
                        }

                        return creatorAccount;
                }
        }

        private void saveContentAndHostedContent(
                        Connection connection,
                        CreatorAccount creatorAccount,
                        Video videoMetadata)
                        throws SQLException {

                boolean originalAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);

                try (
                                DaoTransactional<Content, UUID> contentDao = DAOFactory.createTransactionalDAO(
                                                connection,
                                                Content.class);

                                DaoTransactional<HostedContent, UUID> hostedContentDao = DAOFactory
                                                .createTransactionalDAO(
                                                                connection,
                                                                HostedContent.class)) {

                        Content content = createContent(
                                        videoMetadata,
                                        creatorAccount.getCreator());

                        contentDao.add(content);

                        HostedContent hostedContent = createHostedContent(
                                        videoMetadata,
                                        content,
                                        creatorAccount);

                        hostedContentDao.add(hostedContent);
                        connection.commit();
                } catch (SQLException | RuntimeException exception) {
                        connection.rollback();
                        throw exception;
                } finally {
                        connection.setAutoCommit(originalAutoCommit);
                }
        }

        private Content createContent(
                        Video videoMetadata,
                        Creator creator) {

                Instant now = Instant.now();

                return new Content(
                                UUID.randomUUID(),
                                creator,
                                ContentType.OTHER, // TODO: get this type from user input
                                videoMetadata.getTitle(),
                                videoMetadata.getDurationSeconds(),
                                videoMetadata.getDescription(),
                                true,
                                videoMetadata.getPublishedAt(),
                                videoMetadata.isMembersOnly(),
                                now,
                                now);
        }

        private HostedContent createHostedContent(
                        Video videoMetadata,
                        Content content,
                        CreatorAccount creatorAccount) {

                return new HostedContent(
                                UUID.randomUUID(),
                                content,
                                creatorAccount.getCreator(),
                                creatorAccount,
                                videoMetadata.getId(),
                                Instant.now());
        }
}
