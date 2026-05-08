package nl.lekkeratlas.backendapi.web.authentik;

import io.github.david.auk.fluid.jdbc.components.daos.Dao;
import io.github.david.auk.fluid.jdbc.factories.DAOFactory;
import nl.lekkeratlas.backendapi.web.authentik.dto.AuthentikEventRequest;
import nl.lekkeratlas.backendapi.web.authentik.dto.AuthentikUser;
import nl.lekkeratlas.shared.model.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/webhooks/authentik")
public class AuthentikController {

        private static final Logger log = LoggerFactory.getLogger(AuthentikController.class);

        @Value("${app.webhooks.authentik.debug:false}")
        private boolean debugEnabled;

        @GetMapping
        public ResponseEntity<String> healthCheck() {
                System.out.println("Received Authentik health check");
                return ResponseEntity.ok("OK");
        }

        @PostMapping
        public ResponseEntity<Void> handleAuthentikEvent(@RequestBody AuthentikEventRequest event) throws IOException {

                if (debugEnabled) log.info("Received Authentik event {}", event);

                AuthentikUser authentikUser = event.user();
                User user = new User(authentikUser);

                try (Dao<User, UUID> userDao = DAOFactory.createDAO(User.class)) {

                        User fetchedUser = userDao.get(user.getId());

                        if (fetchedUser == null) {
                                userDao.add(user);
                                return ResponseEntity.ok().build();
                        }

                        userDao.update(user);
                        return ResponseEntity.ok().build();
                }
        }
}
