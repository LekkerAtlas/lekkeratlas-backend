package nl.lekkeratlas.backendapi.web.authentik.dto;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

public record AuthentikUser(
                String uuid,
                String username,

                // Display name
                String name,

                String email,

                Instant date_joined,
                Instant last_updated,
                Instant last_login) {

        public UUID formatUuid() throws IOException {
                String raw = uuid;

                if (raw == null || raw.isBlank()) {
                        return null;
                }

                raw = raw.trim();

                if (raw.length() == 32) {
                        raw = raw.substring(0, 8) + "-" +
                                        raw.substring(8, 12) + "-" +
                                        raw.substring(12, 16) + "-" +
                                        raw.substring(16, 20) + "-" +
                                        raw.substring(20);
                }

                try {
                        return UUID.fromString(raw);
                } catch (IllegalArgumentException exception) {
                        throw new IOException(
                                        "Expected a UUID in either standard 36-character format or compact 32-character format");
                }
        }
}
