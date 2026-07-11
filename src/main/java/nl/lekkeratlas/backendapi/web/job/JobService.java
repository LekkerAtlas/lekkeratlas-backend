package nl.lekkeratlas.backendapi.web.job;

import java.sql.Connection;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.david.auk.fluid.jdbc.components.daos.Dao;
import io.github.david.auk.fluid.jdbc.factories.DAOFactory;
import nl.lekkeratlas.backendapi.exceptions.JobAlreadyFinishedException;
import nl.lekkeratlas.backendapi.exceptions.QueueJobException;
import nl.lekkeratlas.shared.model.queue.QueueJob;
import nl.lekkeratlas.shared.model.queue.QueueJobCancellationRequest;
import nl.lekkeratlas.shared.model.user.User;

@Service
// @RequiredArgsConstructor
public class JobService {

        @Transactional
        public void cancelJob(Connection connection, User user, UUID jobId) throws QueueJobException {

                try (Dao<QueueJob, UUID> queueJobDao = DAOFactory.createDAO(connection, QueueJob.class)) {

                        try (Dao<QueueJobCancellationRequest, UUID> queueJobCancellationRequestDao = DAOFactory
                                        .createDAO(connection,
                                                        QueueJobCancellationRequest.class)) {
                                QueueJob queueJob = queueJobDao.get(jobId);

                                if (queueJob == null)
                                        throw new QueueJobException("Queue job " + jobId + " not found");

                                if (!user.getId().equals(queueJob.getRequestedBy().getId()))
                                        throw new QueueJobException("User did not create this QueueJob");

                                switch (queueJob.getStatus()) {
                                        case COMPLETED:
                                                throw new JobAlreadyFinishedException(queueJob);
                                        case FAILED:
                                                throw new JobAlreadyFinishedException(queueJob);
                                        case CANCELED:
                                                return;
                                        default:
                                                createCanceledQueueJobEvent(queueJobCancellationRequestDao, queueJob);
                                }
                        }
                }
        }

        public void createCanceledQueueJobEvent(Dao<QueueJobCancellationRequest, UUID> queueJobCancellationRequestDao,
                        QueueJob queueJob) {

                QueueJobCancellationRequest queueJobCancellationRequest = new QueueJobCancellationRequest(queueJob,
                                Instant.now());

                queueJobCancellationRequestDao.add(queueJobCancellationRequest);
        }
}
