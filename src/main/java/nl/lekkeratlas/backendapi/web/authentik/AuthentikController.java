package nl.lekkeratlas.backendapi.web.authentik;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.david.auk.fluid.jdbc.components.daos.Dao;
import io.github.david.auk.fluid.jdbc.factories.DAOFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import nl.lekkeratlas.backendapi.web.authentik.dto.AuthentikEventRequest;
import nl.lekkeratlas.backendapi.web.authentik.dto.AuthentikUser;
import nl.lekkeratlas.shared.model.user.User;

@Tag(name = "Authentik Webhooks", description = "Internal webhook endpoints used by the Authentik identity provider to synchronize users into the LekkerAtlas backend. These endpoints are intended for server-to-server communication only and are protected using a shared webhook secret and internal network restrictions.")
@RestController
@RequestMapping("/webhooks/authentik")
public class AuthentikController {

        private static final Logger log = LoggerFactory.getLogger(AuthentikController.class);

        @Value("${app.webhooks.authentik.debug:false}")
        private boolean debugEnabled;

        @Operation(summary = "Synchronize Authentik user", description = "Receives Authentik webhook events and synchronizes the provided user into the LekkerAtlas database. Existing users are updated and unknown users are automatically created. This endpoint is intended for Authentik server-to-server webhook communication only.", security = {
                        @SecurityRequirement(name = "bearerAuth")
        })
        @ApiResponse(responseCode = "200", description = "Webhook processed successfully")
        @ApiResponse(responseCode = "401", description = "Missing or invalid webhook secret")
        @ApiResponse(responseCode = "403", description = "Webhook source is not allowed")
        @ApiResponse(responseCode = "500", description = "Internal synchronization error")
        @PostMapping
        public ResponseEntity<Void> handleAuthentikEvent(
                        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Authentik webhook payload containing the synchronized user information.", required = true, content = @Content(schema = @Schema(implementation = AuthentikEventRequest.class), examples = @ExampleObject(name = "User Sync Event", value = """
                                        {
                                          "action": "authentik_user_sync",
                                          "user": {
                                            "uuid": "a23eff14-daaf-4c3d-a4bb-48500dde99e5",
                                            "username": "akadmin",
                                            "email": "admin@example.com",
                                            "name": "Authentik Admin"
                                          }
                                        }
                                        """))) @RequestBody AuthentikEventRequest event)
                        throws IOException {

                if (debugEnabled)
                        log.info("Received Authentik event {}", event);

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
