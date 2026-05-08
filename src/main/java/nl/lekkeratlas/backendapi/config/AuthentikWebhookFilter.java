package nl.lekkeratlas.backendapi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class AuthentikWebhookFilter extends OncePerRequestFilter {

        private static final Logger logger = LoggerFactory.getLogger(AuthentikWebhookFilter.class);

        @Value("${app.webhooks.authentik.secret}")
        private String expectedSecret;

        @Value("${app.webhooks.authentik.debug:false}")
        private boolean debugEnabled;

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
                return !request.getRequestURI().equals("/webhooks/authentik");
        }

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                @NonNull HttpServletResponse response,
                @NonNull FilterChain filterChain
        ) throws ServletException, IOException {
                String authorization = request.getHeader("Authorization");
                if (debugEnabled) {
                        logger.info("Authentik webhook request received");
                        logger.info("Method: {}", request.getMethod());
                        logger.info("URI: {}", request.getRequestURI());
                        logger.info("Authorization header present: {}", authorization != null);

                        if (authorization != null) {
                                logger.info("Authorization header value: {}", authorization);
                        }
                }

                String expected = "Bearer " + expectedSecret;

                if (!constantTimeEquals(authorization, expected)) {
                        logger.info("Authentik webhook authentication failed");

                        if (debugEnabled) {
                                logger.info("Expected Authorization header: {}", expected);
                                logger.info("Received Authorization header: {}", authorization);
                        }
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                }

                if (debugEnabled) logger.info("Authentik webhook authentication succeeded");

                filterChain.doFilter(request, response);
        }

        private boolean constantTimeEquals(String actual, String expected) {
                if (actual == null) return false;

                return MessageDigest.isEqual(
                        actual.getBytes(StandardCharsets.UTF_8),
                        expected.getBytes(StandardCharsets.UTF_8)
                );
        }
}