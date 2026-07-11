package nl.lekkeratlas.shared.rabbit;

import java.sql.Connection;
import java.util.UUID;

import org.springframework.stereotype.Service;

import io.github.david.auk.fluid.jdbc.components.daos.Dao;
import io.github.david.auk.fluid.jdbc.factories.DAOFactory;
import nl.lekkeratlas.shared.model.queue.QueueJob;
import nl.lekkeratlas.shared.model.queue.QueueJobCancellationRequest;

@Service
public class WorkCommandEventSync {

        public boolean isCanceled(
                        Connection connection,
                        QueueJob queueJob) {

                // Check if there is a new cancellation request
                if (isNewCancelationRequest(connection, queueJob))
                        return true;

                // Check if the job is already cancelled
                QueueJob latestQueueJob = update(connection, queueJob);

                return latestQueueJob.isCanceled();
        }

        private QueueJob update(
                        Connection connection,
                        QueueJob queueJob) {
                try (Dao<QueueJob, UUID> queueJobDao = DAOFactory.createDAO(connection, QueueJob.class)) {
                        return queueJobDao.get(queueJob.getId());
                }
        }

        private boolean isNewCancelationRequest(
                        Connection connection,
                        QueueJob queueJob) {
                try (Dao<QueueJobCancellationRequest, UUID> queueJobCancellationRequestDao = DAOFactory
                                .createDAO(connection, QueueJobCancellationRequest.class)) {
                        return queueJobCancellationRequestDao.existsByPrimaryKey(queueJob.getId());
                }
        }
}
