package nl.lekkeratlas.backendapi.web.command;

import static io.github.david.auk.fluid.jdbc.components.daos.querying.operator.SingleValueOperator.EQUALS;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.david.auk.fluid.jdbc.components.Database;
import io.github.david.auk.fluid.jdbc.components.daos.Dao;
import io.github.david.auk.fluid.jdbc.components.daos.QueryBuilder;
import io.github.david.auk.fluid.jdbc.factories.DAOFactory;
import nl.lekkeratlas.backendapi.exceptions.JWTException;
import nl.lekkeratlas.backendapi.exceptions.QueueJobException;
import nl.lekkeratlas.backendapi.web.Utils;
import nl.lekkeratlas.shared.exceptions.UserNotFoundException;
import nl.lekkeratlas.shared.model.queue.QueueJob;
import nl.lekkeratlas.shared.model.user.User;

@RestController
@RequestMapping("/api/command")
public class CommandController {

        @DeleteMapping("/{commandId}")
        public void deleteCommand(@PathVariable String commandId, JwtAuthenticationToken authenticationToken)
                        throws SQLException, NoSuchFieldException, JWTException, UserNotFoundException,
                        QueueJobException {

                UUID userId = Utils.resolveCurrentUserId(authenticationToken);
                User user;

                try (Connection connection = Database.getConnection()) {

                        try (Dao<User, UUID> userDao = DAOFactory.createDAO(connection, User.class)) {
                                user = userDao.get(userId);
                                if (user == null) {
                                        throw new UserNotFoundException("User not found");
                                }
                        }

                        QueueJob queueJob;

                        try (Dao<QueueJob, UUID> queueJobDao = DAOFactory.createDAO(connection, QueueJob.class)) {
                                queueJob = new QueryBuilder<>(queueJobDao)
                                                .where(QueueJob.class.getDeclaredField("requestedBy"), EQUALS,
                                                                user.getId())
                                                .getUnique();

                                if (queueJob == null) {
                                        throw new QueueJobException("Queue job not found");
                                }
                        }

                }
        }
}
