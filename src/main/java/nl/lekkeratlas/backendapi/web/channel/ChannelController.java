package nl.lekkeratlas.backendapi.web.channel;

import io.github.david.auk.fluid.jdbc.components.Database;
import io.github.david.auk.fluid.jdbc.components.daos.Dao;
import io.github.david.auk.fluid.jdbc.components.daos.QueryBuilder;
import io.github.david.auk.fluid.jdbc.components.daos.querying.operator.SingleValueOperator;
import io.github.david.auk.fluid.jdbc.factories.DAOFactory;
import nl.lekkeratlas.backendapi.exceptions.JWTException;
import nl.lekkeratlas.backendapi.web.Utils;
import nl.lekkeratlas.backendapi.web.dto.AddChannelRequest;
import nl.lekkeratlas.backendapi.web.dto.CommandAcceptedResponse;
import nl.lekkeratlas.shared.command.FetchPlatformContentCommand;
import nl.lekkeratlas.shared.exceptions.UserNotFoundException;
import nl.lekkeratlas.shared.model.queue.QueueJob;
import nl.lekkeratlas.shared.model.queue.QueueJobType;
import nl.lekkeratlas.shared.model.user.User;
import nl.lekkeratlas.shared.rabbit.WorkCommandProducer;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/channels")
public class ChannelController {

        private final WorkCommandProducer workCommandProducer;

        public ChannelController(WorkCommandProducer workCommandProducer) {
                this.workCommandProducer = workCommandProducer;
        }

        @PostMapping
        public ResponseEntity<CommandAcceptedResponse> addChannel(
                        @RequestBody AddChannelRequest request,
                        JwtAuthenticationToken authentication)
                        throws NoSuchFieldException, JWTException, UsernameNotFoundException, UserNotFoundException,
                        SQLException {
                UUID userId = Utils.resolveCurrentUserId(authentication);

                User user;

                try (Connection connection = Database.getConnection()) {
                        try (Dao<User, UUID> userDao = DAOFactory.createDAO(connection, User.class)) {
                                user = userDao.get(userId);

                                if (user == null) {
                                        throw new UserNotFoundException("User not found");
                                }

                                FetchPlatformContentCommand command = new FetchPlatformContentCommand(
                                                request.channelId(),
                                                userId);

                                try (Dao<QueueJob, UUID> queueJobDao = DAOFactory.createDAO(connection,
                                                QueueJob.class)) {
                                        List<QueueJob> existingJobs = new QueryBuilder<>(queueJobDao)
                                                        .where(QueueJob.class.getDeclaredField("dedupeKey"),
                                                                        SingleValueOperator.EQUALS, command.dedupeKey())
                                                        // TODO Add OR when implemented in Fluid JDBC
                                                        // .and(QueueJob.class.getDeclaredField("status"),
                                                        // SingleValueOperator.EQUALS, QueueJobStatus.QUEUED)
                                                        // .or(QueueJob.class.getDeclaredField("status"),
                                                        // SingleValueOperator.EQUALS, QueueJobStatus.RUNNING)
                                                        .get();

                                        QueueJob existingJob = null;

                                        for (QueueJob unfilteredExistingJob : existingJobs) {
                                                if (unfilteredExistingJob.getStatus().isActive()) {
                                                        existingJob = unfilteredExistingJob;
                                                        break;
                                                }
                                        }

                                        if (existingJob != null) {
                                                return ResponseEntity.accepted()
                                                                .body(new CommandAcceptedResponse(existingJob.getId()));
                                        }

                                        QueueJob queueJob = workCommandProducer.publish(
                                                        QueueJobType.FETCH_PLATFORM_CONTENT,
                                                        command,
                                                        null,
                                                        queueJobDao);

                                        return ResponseEntity.accepted()
                                                        .body(new CommandAcceptedResponse(queueJob.getId()));
                                }
                        }
                }
        }
}
