package nl.lekkeratlas.shared.rabbit;

import io.github.david.auk.fluid.jdbc.components.Database;
import io.github.david.auk.fluid.jdbc.components.daos.Dao;
import io.github.david.auk.fluid.jdbc.factories.DAOFactory;
import nl.lekkeratlas.shared.model.queue.QueueJob;
import nl.lekkeratlas.shared.model.queue.QueueJobEvent;
import nl.lekkeratlas.shared.model.queue.QueueJobStatus;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

@Service
public class WorkCommandUpdateProducer {

        public QueueJobEvent update(
                        Connection connection,
                        QueueJob queueJob,
                        QueueJobStatus status,
                        String messageForEndUser) {
                try (Dao<QueueJobEvent, UUID> queueJobEventDao = DAOFactory.createDAO(connection,
                                QueueJobEvent.class)) {
                        return update(queueJobEventDao, queueJob, status, messageForEndUser);
                }
        }

        public QueueJobEvent update(
                        QueueJob queueJob,
                        QueueJobStatus status,
                        String messageForEndUser) {
                try (Connection connection = Database.getConnection()) {
                        return update(connection, queueJob, status, messageForEndUser);
                } catch (SQLException e) {
                        throw new AmqpRejectAndDontRequeueException(e);
                }
        }

        public QueueJobEvent update(
                        Dao<QueueJobEvent, UUID> queueJobEventDao,
                        QueueJob queueJob,
                        QueueJobStatus status,
                        String messageForEndUser) {
                UUID commandId = UUID.randomUUID();

                QueueJobEvent queueJobEvent = new QueueJobEvent(
                                commandId,
                                queueJob,
                                status,
                                messageForEndUser,
                                Instant.now());

                try {
                        queueJobEventDao.add(queueJobEvent);
                } catch (Exception e) {
                        throw new AmqpRejectAndDontRequeueException(e);
                }

                return queueJobEvent;
        }
}
