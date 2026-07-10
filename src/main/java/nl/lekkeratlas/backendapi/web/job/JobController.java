package nl.lekkeratlas.backendapi.web.job;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.david.auk.fluid.jdbc.components.Database;
import io.github.david.auk.fluid.jdbc.components.daos.Dao;
import io.github.david.auk.fluid.jdbc.factories.DAOFactory;
import nl.lekkeratlas.backendapi.exceptions.JsonWebTokenException;
import nl.lekkeratlas.backendapi.exceptions.QueueJobException;
import nl.lekkeratlas.backendapi.web.Utils;
import nl.lekkeratlas.shared.exceptions.UserNotFoundException;
import nl.lekkeratlas.shared.model.user.User;

@RestController
@RequestMapping("/api/job")
public class JobController {

        private final JobService jobService;

        public JobController(JobService jobService) {
                this.jobService = jobService;
        }

        @PostMapping("/{jobId}/cancel")
        // TODO: Parse checked current user via the parameters
        public ResponseEntity<Void> cancelJob(@PathVariable UUID jobId, JwtAuthenticationToken authenticationToken)
                        throws JsonWebTokenException, UserNotFoundException, SQLException, QueueJobException {

                UUID userId = Utils.resolveCurrentUserId(authenticationToken);

                try (Connection connection = Database.getConnection();
                                Dao<User, UUID> userDao = DAOFactory.createDAO(connection, User.class)) {
                        User user = userDao.get(userId);

                        if (user == null) {
                                throw new UserNotFoundException("User not found");
                        }

                        jobService.cancelJob(connection, user, jobId);

                }
                return ResponseEntity.accepted().build();
        }
}
