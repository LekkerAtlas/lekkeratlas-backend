package nl.lekkeratlas.worker.service;

import java.sql.Connection;
import java.util.UUID;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.stereotype.Service;

import io.github.david.auk.fluid.jdbc.components.daos.Dao;
import io.github.david.auk.fluid.jdbc.factories.DAOFactory;
import nl.lekkeratlas.shared.model.user.User;

@Service
public class UserLookupService {

        public User requireExistingUser(Connection connection, UUID userId) {
                try (Dao<User, UUID> userDao = DAOFactory.createDAO(connection, User.class)) {
                        User user = userDao.get(userId);

                        if (user == null) {
                                throw new AmqpRejectAndDontRequeueException("User not found: " + userId);
                        }

                        return user;
                }
        }
}
