package nl.lekkeratlas.shared.rabbit;

import java.sql.Connection;
import java.util.UUID;

import org.springframework.stereotype.Service;

import io.github.david.auk.fluid.jdbc.components.daos.Dao;
import io.github.david.auk.fluid.jdbc.factories.DAOFactory;
import nl.lekkeratlas.shared.model.queue.QueueJob;

@Service
public class WorkCommandEventSync {

        public QueueJob update(
                        Connection connection,
                        QueueJob queueJob) {
                try (Dao<QueueJob, UUID> queueJobDao = DAOFactory.createDAO(connection, QueueJob.class)) {
                        return queueJobDao.get(queueJob.getId());
                }
        }

        public boolean isCanceled(
                        Connection connection,
                        QueueJob queueJob) {
                QueueJob latestQueueJob = update(connection, queueJob);

                return latestQueueJob.isCanceled();
        }
}
