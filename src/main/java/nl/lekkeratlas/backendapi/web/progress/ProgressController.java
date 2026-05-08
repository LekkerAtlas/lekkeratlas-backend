package nl.lekkeratlas.backendapi.web.progress;

import io.github.david.auk.fluid.jdbc.components.Database;
import io.github.david.auk.fluid.jdbc.components.daos.Dao;
import io.github.david.auk.fluid.jdbc.components.daos.QueryBuilder;
import io.github.david.auk.fluid.jdbc.factories.DAOFactory;
import nl.lekkeratlas.backendapi.web.Utils;
import nl.lekkeratlas.backendapi.web.dto.GetProgressResponse;
import nl.lekkeratlas.backendapi.web.dto.Progress;
import nl.lekkeratlas.backendapi.web.dto.ProgressStatusEvent;
import nl.lekkeratlas.shared.model.queue.QueueJob;
import nl.lekkeratlas.shared.model.queue.QueueJobEvent;
import nl.lekkeratlas.shared.model.user.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static io.github.david.auk.fluid.jdbc.components.daos.querying.operator.SingleValueOperator.EQUALS;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

        @GetMapping("/{queueJobId}")
        public ResponseEntity<GetProgressResponse> getProgress(@PathVariable UUID queueJobId, JwtAuthenticationToken authenticationToken) throws SQLException, NoSuchFieldException {

                validateQueueJobId(queueJobId);

                UUID userId = Utils.resolveCurrentUserId(authenticationToken);

                try (Connection connection = Database.getConnection()) {
                        User user = getCurrentUser(connection, userId);
                        QueueJob queueJob = getQueueJobForUser(connection, queueJobId, user, userId);

                        return ResponseEntity.ok(new GetProgressResponse(
                                buildProgress(connection, queueJob)
                        ));
                }
        }

        private void validateQueueJobId(UUID queueJobId) {
                if (queueJobId == null) {
                        throw new RuntimeException("Queue job id is required");
                }
        }

        private User getCurrentUser(Connection connection, UUID userId) throws SQLException {
                try (Dao<User, UUID> userDao = DAOFactory.createDAO(connection, User.class)) {
                        User user = userDao.get(userId);

                        if (user == null) {
                                throw new RuntimeException("User not found");
                        }

                        return user;
                }
        }

        private QueueJob getQueueJobForUser(Connection connection, UUID queueJobId, User user, UUID userId) throws SQLException, NoSuchFieldException {
                try (Dao<QueueJob, UUID> queueJobDao = DAOFactory.createDAO(connection, QueueJob.class)) {
                        QueueJob queueJob = new QueryBuilder<>(queueJobDao)
                                .where(QueueJob.class.getDeclaredField("id"), EQUALS, queueJobId)
                                .and(QueueJob.class.getDeclaredField("requestedBy"), EQUALS, user.getId())
                                .getUnique();

                        if (queueJob == null) {
                                throw new RuntimeException("Queue job " + queueJobId +
                                        " not found for user " + userId);
                        }

                        return queueJob;
                }
        }

        private Progress buildProgress(Connection connection, QueueJob queueJob) throws SQLException, NoSuchFieldException {
                List<QueueJobEvent> events = getQueueJobEvents(connection, queueJob);
                List<Progress> childProgresses = getChildQueueJobs(connection, queueJob)
                        .stream()
                        .map(childQueueJob -> {
                                try {
                                        return buildProgress(connection, childQueueJob);
                                } catch (SQLException | NoSuchFieldException exception) {
                                        throw new RuntimeException(exception);
                                }
                        })
                        .toList();

                return new Progress(
                        queueJob.getId(),
                        queueJob.getStatus(),
                        ProgressStatusEvent.from(events),
                        childProgresses
                );
        }

        private List<QueueJobEvent> getQueueJobEvents(Connection connection, QueueJob queueJob) throws SQLException, NoSuchFieldException {
                try (Dao<QueueJobEvent, UUID> queueJobEventDao = DAOFactory.createDAO(connection, QueueJobEvent.class)) {
                        return queueJobEventDao.get(QueueJobEvent.class.getDeclaredField("job"), EQUALS, queueJob.getId());
                }
        }

        private List<QueueJob> getChildQueueJobs(Connection connection, QueueJob queueJob) throws SQLException, NoSuchFieldException {
                try (Dao<QueueJob, UUID> queueJobDao = DAOFactory.createDAO(connection, QueueJob.class)) {
                        return queueJobDao.get(QueueJob.class.getDeclaredField("parentJob"), EQUALS, queueJob.getId());
                }
        }
}
