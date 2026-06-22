package nl.lekkeratlas.worker.service;

import java.sql.Connection;
import java.util.UUID;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.stereotype.Service;

import io.github.david.auk.fluid.jdbc.components.daos.Dao;
import io.github.david.auk.fluid.jdbc.factories.DAOFactory;
import nl.lekkeratlas.shared.model.queue.QueueJob;
import nl.lekkeratlas.shared.model.queue.QueueJobStatus;

@Service
public class QueueJobLookupService {

        public QueueJob requireQueueJob(Connection connection, UUID queueJobId) {
                try (Dao<QueueJob, UUID> queueJobDao = DAOFactory.createDAO(connection, QueueJob.class)) {
                        QueueJob queueJob = queueJobDao.get(queueJobId);

                        if (queueJob == null) {
                                throw new AmqpRejectAndDontRequeueException("Queue job not found: " + queueJobId);
                        }

                        return queueJob;
                }
        }

        public QueueJob requireCleanQueueJob(Connection connection, UUID uuid) {
                QueueJob queueJob = requireQueueJob(connection, uuid);
                if (queueJob.getStatus() != QueueJobStatus.QUEUED) {
                        throw new AmqpRejectAndDontRequeueException("Queue job is not in queued state: " + uuid);
                }
                return queueJob;
        }
}
