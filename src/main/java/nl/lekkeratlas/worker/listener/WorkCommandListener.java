package nl.lekkeratlas.worker.listener;

import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.ImmediateRequeueAmqpException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import io.github.david.auk.fluid.jdbc.components.daos.Dao;
import io.github.david.auk.fluid.jdbc.factories.DAOFactory;
import nl.lekkeratlas.shared.command.FetchPlatformContentCommand;
import nl.lekkeratlas.shared.command.FetchVideoMetadataCommand;
import nl.lekkeratlas.shared.command.WorkCommandEnvelope;
import nl.lekkeratlas.shared.model.queue.QueueJob;
import nl.lekkeratlas.shared.model.queue.QueueJobStatus;
import nl.lekkeratlas.shared.rabbit.RabbitNames;
import nl.lekkeratlas.shared.rabbit.WorkCommandUpdateProducer;
import nl.lekkeratlas.worker.exceptions.QueueJobException;
import nl.lekkeratlas.worker.execution.QueueJobCancellationToken;
import nl.lekkeratlas.worker.execution.QueueJobExecutionService;
import nl.lekkeratlas.worker.handler.FetchPlatformContentCommandHandler;
import nl.lekkeratlas.worker.handler.FetchVideoMetadataCommandHandler;
import tools.jackson.databind.ObjectMapper;

/**
 * Generic listener for the shared work queue.
 *
 * <p>
 * The RabbitMQ listener thread only receives and coordinates messages.
 * Actual command execution takes place on a dedicated queue-job worker thread.
 */
@Component
public class WorkCommandListener {

        private static final Logger logger = LoggerFactory.getLogger(WorkCommandListener.class);

        private final ObjectMapper objectMapper;
        private final FetchPlatformContentCommandHandler fetchPlatformContentCommandHandler;
        private final FetchVideoMetadataCommandHandler fetchVideoMetadataCommandHandler;
        private final WorkCommandUpdateProducer workCommandUpdateProducer;
        private final QueueJobExecutionService queueJobExecutionService;

        public WorkCommandListener(
                        ObjectMapper objectMapper,
                        FetchPlatformContentCommandHandler fetchPlatformContentCommandHandler,
                        FetchVideoMetadataCommandHandler fetchVideoMetadataCommandHandler,
                        WorkCommandUpdateProducer workCommandUpdateProducer,
                        QueueJobExecutionService queueJobExecutionService) {

                this.objectMapper = objectMapper;
                this.fetchPlatformContentCommandHandler = fetchPlatformContentCommandHandler;
                this.fetchVideoMetadataCommandHandler = fetchVideoMetadataCommandHandler;
                this.workCommandUpdateProducer = workCommandUpdateProducer;
                this.queueJobExecutionService = queueJobExecutionService;
        }

        @RabbitListener(queues = RabbitNames.WORK_QUEUE)
        public void handle(WorkCommandEnvelope envelope) {
                QueueJob queueJob = requireQueueJob(envelope.commandId());

                try {
                        queueJobExecutionService.executeAndWait(
                                        queueJob,
                                        cancellation -> dispatch(
                                                        envelope,
                                                        cancellation));
                } catch (ImmediateRequeueAmqpException exception) {
                        /*
                         * The Rabbit listener thread itself was interrupted,
                         * for example during application shutdown.
                         *
                         * Spring AMQP must receive this exception so it can
                         * requeue the message.
                         */
                        throw exception;
                } catch (QueueJobException exception) {
                        handleQueueJobException(exception);
                } catch (Exception exception) {
                        handleUnexpectedException(
                                        queueJob,
                                        exception);
                }
        }

        private void dispatch(
                        WorkCommandEnvelope envelope,
                        QueueJobCancellationToken cancellation)
                        throws Exception {

                switch (envelope.type()) {
                        case FETCH_PLATFORM_CONTENT ->
                                handleFetchPlatformContent(
                                                envelope,
                                                cancellation);

                        case FETCH_VIDEO_METADATA ->
                                handleFetchVideoMetadata(
                                                envelope,
                                                cancellation);

                        default ->
                                throw new NotImplementedException(
                                                "No handler configured for: "
                                                                + envelope.type());
                }
        }

        private void handleFetchPlatformContent(
                        WorkCommandEnvelope envelope,
                        QueueJobCancellationToken cancellation)
                        throws QueueJobException,
                        SQLException,
                        NoSuchFieldException {

                FetchPlatformContentCommand command = objectMapper.convertValue(
                                envelope.payload(),
                                FetchPlatformContentCommand.class);

                fetchPlatformContentCommandHandler.handle(
                                envelope,
                                command,
                                cancellation);
        }

        private void handleFetchVideoMetadata(
                        WorkCommandEnvelope envelope,
                        QueueJobCancellationToken cancellation)
                        throws QueueJobException {

                FetchVideoMetadataCommand command = objectMapper.convertValue(
                                envelope.payload(),
                                FetchVideoMetadataCommand.class);

                fetchVideoMetadataCommandHandler.handle(
                                envelope,
                                command,
                                cancellation);
        }

        private QueueJob requireQueueJob(UUID commandId) {
                try (Dao<QueueJob, UUID> queueJobDao = DAOFactory.createDAO(QueueJob.class)) {

                        QueueJob queueJob = queueJobDao.get(commandId);

                        if (queueJob == null) {
                                throw new AmqpRejectAndDontRequeueException(
                                                "No queue job found for command "
                                                                + commandId);
                        }

                        return queueJob;
                } catch (AmqpRejectAndDontRequeueException exception) {
                        throw exception;
                } catch (Exception exception) {
                        /*
                         * This might be a temporary database problem.
                         * Requeue the message instead of permanently rejecting it.
                         */
                        throw new ImmediateRequeueAmqpException(
                                        "Could not load queue job "
                                                        + commandId,
                                        exception);
                }
        }

        private void handleQueueJobException(
                        QueueJobException exception) {

                QueueJobStatus status = Objects.requireNonNull(
                                exception.getStatus(),
                                "QueueJobException status cannot be null");

                QueueJob queueJob = Objects.requireNonNull(
                                exception.getQueueJob(),
                                "QueueJobException queue job cannot be null");

                switch (status) {
                        case FAILED ->
                                logger.warn(
                                                "Failed queue job: {}",
                                                queueJob.getId());

                        case CANCELED ->
                                logger.info(
                                                "Canceled queue job: {}",
                                                queueJob.getId());

                        default ->
                                logger.info(
                                                "Queue job {} ended with status {}",
                                                queueJob.getId(),
                                                status);
                }

                /*
                 * This exception has now been handled. Returning normally from
                 * the listener lets RabbitMQ acknowledge the original message.
                 */
                workCommandUpdateProducer.update(
                                queueJob,
                                status,
                                exception.getMessageForEndUser());
        }

        private void handleUnexpectedException(
                        QueueJob queueJob,
                        Exception exception) {

                logger.error(
                                "Unexpected error while handling queue job {}",
                                queueJob.getId(),
                                exception);

                workCommandUpdateProducer.update(
                                queueJob,
                                QueueJobStatus.FAILED,
                                buildInternalErrorMessage(exception));
        }

        private String buildInternalErrorMessage(
                        Exception exception) {

                String technicalMessage = exception.getMessage() == null
                                ? exception.getClass().getSimpleName()
                                : exception.getMessage();

                return "There was an internal server error. "
                                + "Please send this to an administrator: "
                                + technicalMessage;
        }
}
