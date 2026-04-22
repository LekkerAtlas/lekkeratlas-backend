package nl.lekkeratlas.lekkeratlasbackend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = {
                "AUTHENTIK_HOST=http://localhost:9000",
                "AUTHENTIK_CLIENT_ID=test-client-id",
                "AUTHENTIK_CLIENT_SECRET=test-client-secret"
        },
        classes = LekkeratlasBackendApplication.class
)
@org.springframework.boot.autoconfigure.EnableAutoConfiguration(exclude = {
        OAuth2ClientAutoConfiguration.class,
        OAuth2ResourceServerAutoConfiguration.class
})
@Disabled("Temporarily disabled")
class LekkeratlasBackendApplicationTests {

    @Test
    void contextLoads() {
    }
}