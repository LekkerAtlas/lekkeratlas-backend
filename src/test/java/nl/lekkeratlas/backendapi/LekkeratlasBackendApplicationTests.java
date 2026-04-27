package nl.lekkeratlas.backendapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        classes = LekkeratlasBackendApplication.class,
        properties = {
                "spring.profiles.active=backend,dev",
                "spring.rabbitmq.listener.simple.auto-startup=false"
        }
)
class LekkeratlasBackendApplicationTests {

    @Test
    void contextLoads() {
    }
}