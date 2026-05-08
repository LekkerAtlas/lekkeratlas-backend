package nl.lekkeratlas.backendapi.web;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

public class Utils {
        private Utils() {
                /* This utility class should not be instantiated */
        }


        /**
         * Temporary placeholder.
         * <p>
         * Later this should come from Spring Security / Authentik claims.
         */
        public static UUID resolveCurrentUserId(JwtAuthenticationToken authentication) {
                String subject = authentication.getToken().getSubject();

                if (subject == null || subject.isBlank()) {
                        throw new RuntimeException("Authenticated JWT does not contain a subject claim");
                }

                return parseUuidSubject(subject);
        }


        private static UUID parseUuidSubject(String subject) {
                if (subject.length() == 32) {
                        return UUID.fromString(
                                subject.substring(0, 8) + "-" +
                                        subject.substring(8, 12) + "-" +
                                        subject.substring(12, 16) + "-" +
                                        subject.substring(16, 20) + "-" +
                                        subject.substring(20)
                        );
                }

                return UUID.fromString(subject);
        }
}
