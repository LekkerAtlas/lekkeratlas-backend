package nl.lekkeratlas.backendapi.config;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthentikWebhookFilter extends OncePerRequestFilter {

        private static final Logger LOGGER = LoggerFactory.getLogger(AuthentikWebhookFilter.class);

        @Value("${app.webhooks.authentik.secret}")
        private String expectedSecret;

        @Value("${app.webhooks.authentik.debug:false}")
        private boolean debugEnabled;

        @Value("${app.webhooks.authentik.allowed-source-cidrs}")
        private List<String> allowedSourceCidrs;

        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
                return !request.getRequestURI().equals("/webhooks/authentik");
        }

        @Override
        protected void doFilterInternal(
                        HttpServletRequest request,
                        @NonNull HttpServletResponse response,
                        @NonNull FilterChain filterChain) throws ServletException, IOException {
                String authorization = request.getHeader("Authorization");
                String remoteAddress = resolveRemoteAddress(request);
                if (debugEnabled) {
                        LOGGER.info("Authentik webhook request received");
                        LOGGER.info("Method: {}", request.getMethod());
                        LOGGER.info("URI: {}", request.getRequestURI());
                        LOGGER.info("Remote address: {}", remoteAddress);
                        LOGGER.info("Allowed source CIDRs: {}", allowedSourceCidrs);
                        LOGGER.info("Authorization header present: {}", authorization != null);

                        // Scary
                        if (authorization != null) {
                                LOGGER.info("Authorization header value: {}", authorization);
                        }
                }

                if (!isAllowedSource(remoteAddress)) {
                        LOGGER.info("Authentik webhook rejected because source address is not allowed: {}",
                                        remoteAddress);
                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                        return;
                }

                String expected = "Bearer " + expectedSecret;

                if (!constantTimeEquals(authorization, expected)) {
                        LOGGER.info("Authentik webhook authentication failed");

                        if (debugEnabled) {
                                LOGGER.info("Authorization header present: {}", authorization != null);
                        }
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                }

                if (debugEnabled)
                        LOGGER.info("Authentik webhook authentication succeeded");

                filterChain.doFilter(request, response);
        }

        private String resolveRemoteAddress(HttpServletRequest request) {
                return request.getRemoteAddr();
        }

        private boolean isAllowedSource(String remoteAddress) {
                if (remoteAddress == null || remoteAddress.isBlank())
                        return false;

                try {
                        InetAddress address = InetAddress.getByName(remoteAddress);

                        return allowedSourceCidrs.stream()
                                        .map(String::trim)
                                        .filter(cidr -> !cidr.isBlank())
                                        .anyMatch(cidr -> isAddressInCidr(address, cidr));
                } catch (UnknownHostException exception) {
                        LOGGER.info("Could not parse Authentik webhook source address: {}", remoteAddress);
                        return false;
                }
        }

        private boolean isAddressInCidr(InetAddress address, String cidr) {
                String[] parts = cidr.split("/");
                if (parts.length != 2)
                        return false;

                try {
                        InetAddress networkAddress = InetAddress.getByName(parts[0]);
                        int prefixLength = Integer.parseInt(parts[1]);

                        byte[] addressBytes = address.getAddress();
                        byte[] networkBytes = networkAddress.getAddress();

                        if (addressBytes.length != networkBytes.length)
                                return false;

                        int fullBytes = prefixLength / 8;
                        int remainingBits = prefixLength % 8;

                        for (int index = 0; index < fullBytes; index++) {
                                if (addressBytes[index] != networkBytes[index])
                                        return false;
                        }

                        if (remainingBits == 0)
                                return true;

                        int mask = (-1 << (8 - remainingBits)) & 0xff;
                        return (addressBytes[fullBytes] & mask) == (networkBytes[fullBytes] & mask);
                } catch (UnknownHostException | NumberFormatException exception) {
                        LOGGER.info("Could not parse allowed Authentik webhook CIDR: {}", cidr);
                        return false;
                }
        }

        private boolean constantTimeEquals(String actual, String expected) {
                if (actual == null)
                        return false;

                return MessageDigest.isEqual(
                                actual.getBytes(StandardCharsets.UTF_8),
                                expected.getBytes(StandardCharsets.UTF_8));
        }
}
