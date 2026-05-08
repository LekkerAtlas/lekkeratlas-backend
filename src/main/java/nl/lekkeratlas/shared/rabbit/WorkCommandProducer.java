package nl.lekkeratlas.shared.rabbit;

import io.github.david.auk.fluid.jdbc.components.daos.Dao;
import nl.lekkeratlas.shared.command.Command;
import nl.lekkeratlas.shared.command.WorkCommandEnvelope;
import nl.lekkeratlas.shared.model.queue.QueueJob;
import nl.lekkeratlas.shared.model.queue.QueueJobStatus;
import nl.lekkeratlas.shared.model.queue.QueueJobType;
import nl.lekkeratlas.shared.model.user.User;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class WorkCommandProducer {

        private final RabbitTemplate rabbitTemplate;

        public WorkCommandProducer(
                RabbitTemplate rabbitTemplate
        ) {
                this.rabbitTemplate = rabbitTemplate;
        }

        public QueueJob publish(
                QueueJobType jobType,
                Command command,
                QueueJob parentQueueJob,
                Dao<QueueJob, UUID> queueJobDao
        ) {
                UUID commandId = UUID.randomUUID();

                QueueJob queueJob = new QueueJob(
                        commandId,
                        parentQueueJob,
                        jobType,
                        QueueJobStatus.QUEUED,
                        command.serialize(),
                        // Create a mocking user object that only has the user ID. (for putting into the database)
                        User.getDummyUser(command.requestedByUserId()),
                        command.correlationKey(),
                        command.dedupeKey(),
                        null,
                        null,
                        Instant.now(),
                        null,
                        null
                );

                // Add to db
                queueJobDao.add(queueJob);

                // Publish to RabbitMQ
                rabbitTemplate.convertAndSend(
                        RabbitNames.WORK_EXCHANGE,
                        RabbitNames.WORK_ROUTING_KEY,
                        new WorkCommandEnvelope(queueJob)
                );
                return queueJob;
        }
}