package nl.lekkeratlas.worker.listener;

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
import nl.lekkeratlas.worker.handler.FetchPlatformContentCommandHandler;
import nl.lekkeratlas.worker.handler.FetchVideoMetadataCommandHandler;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

/**
 * Generic listener for the single shared work queue.
 * <p>
 * It receives a WorkCommandEnvelope and dispatches the payload to the correct
 * command handler based on the envelope type.
 */
@Component
public class WorkCommandListener {

        private final ObjectMapper objectMapper;
        private final FetchPlatformContentCommandHandler fetchPlatformContentCommandHandler;
        private final FetchVideoMetadataCommandHandler fetchVideoMetadataCommandHandler;
        private final WorkCommandUpdateProducer workCommandUpdateProducer;

        private static final Logger logger = LoggerFactory.getLogger(WorkCommandListener.class);


        public WorkCommandListener(
                ObjectMapper objectMapper,
                FetchPlatformContentCommandHandler fetchPlatformContentCommandHandler,
                FetchVideoMetadataCommandHandler fetchVideoMetadataCommandHandler, WorkCommandUpdateProducer workCommandUpdateProducer
        ) {
                this.objectMapper = objectMapper;
                this.fetchPlatformContentCommandHandler = fetchPlatformContentCommandHandler;
                this.fetchVideoMetadataCommandHandler = fetchVideoMetadataCommandHandler;
                this.workCommandUpdateProducer = workCommandUpdateProducer;
        }

        @RabbitListener(queues = RabbitNames.WORK_QUEUE)
        public void handle(WorkCommandEnvelope envelope) {

                try {
                        switch (envelope.type()) {
                                case FETCH_PLATFORM_CONTENT -> handleFetchPlatformContent(envelope);
                                case FETCH_VIDEO_METADATA -> handleFetchVideoMetadata(envelope);
                                default -> throw new NotImplementedException("No handler configured for: " + envelope);
                        }
                } catch (QueueJobException queueJobException) {
                        if (Objects.requireNonNull(queueJobException.getStatus()) == QueueJobStatus.FAILED) {
                                logger.warn("Failed queue job: {}", queueJobException.getQueueJob().getId());
                        } else if (queueJobException.getStatus() == QueueJobStatus.CANCELED) {
                                logger.info("Canceled queue job: {}", queueJobException.getQueueJob().getId());
                        }

                        workCommandUpdateProducer.update(
                                queueJobException.getQueueJob(),
                                queueJobException.getStatus(),
                                queueJobException.getMessageForEndUser()
                        );
                } catch (Exception e) {

                        logger.error("Error while handling work command", e);

                        try (Dao<QueueJob, UUID> queueJobDao = DAOFactory.createDAO(QueueJob.class)) {
                                QueueJob queueJob = queueJobDao.get(envelope.commandId());

                                if (queueJob == null) {
                                        throw new AmqpRejectAndDontRequeueException(e);
                                }

                                logger.error("Failed queue job: {}", queueJob.getId());

                                workCommandUpdateProducer.update(
                                        queueJob,
                                        QueueJobStatus.FAILED,
                                        "There was an internal server error, " +
                                                "please send this to an administrator: " + e.getMessage()
                                );
                        }
                }
        }

        private void handleFetchPlatformContent(WorkCommandEnvelope envelope) throws QueueJobException, SQLException {
                FetchPlatformContentCommand command = objectMapper.convertValue(
                        envelope.payload(),
                        FetchPlatformContentCommand.class
                );

                fetchPlatformContentCommandHandler.handle(envelope, command);
        }

        private void handleFetchVideoMetadata(WorkCommandEnvelope envelope) throws QueueJobException {
                FetchVideoMetadataCommand command = objectMapper.convertValue(
                        envelope.payload(),
                        FetchVideoMetadataCommand.class
                );

                fetchVideoMetadataCommandHandler.handle(envelope, command);
        }
}