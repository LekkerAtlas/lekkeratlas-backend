package nl.lekkeratlas.worker.service;

import io.github.david.auk.fluid.jdbc.components.daos.Dao;
import io.github.david.auk.fluid.jdbc.factories.DAOFactory;
import nl.lekkeratlas.shared.model.user.User;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

@Service
public class UserLookupService {

        public User requireExistingUser(Connection connection, UUID userId) throws SQLException {
                try (Dao<User, UUID> userDao = DAOFactory.createDAO(connection, User.class)) {
                        User user = userDao.get(userId);

                        if (user == null) {
                                throw new AmqpRejectAndDontRequeueException("User not found: " + userId);
                        }

                        return user;
                }
        }
}