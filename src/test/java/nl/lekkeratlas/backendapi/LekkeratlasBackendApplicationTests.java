package nl.lekkeratlas.backendapi;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = LekkeratlasBackendApplication.class,
        properties = {
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration," +
                        "org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration",
                "spring.rabbitmq.listener.simple.auto-startup=false"
        }
)
@ActiveProfiles("backend")
class LekkeratlasBackendApplicationTests {

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void contextLoads() {
    }
}