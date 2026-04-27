package nl.lekkeratlas.worker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = WorkerApplication.class,
        properties = {
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration," +
                        "org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration",
                "spring.rabbitmq.listener.simple.auto-startup=false",
                "spring.main.web-application-type=none"
        }
)
@ActiveProfiles("worker")
class WorkerApplicationTests {

    @Test
    void contextLoads() {
    }
}