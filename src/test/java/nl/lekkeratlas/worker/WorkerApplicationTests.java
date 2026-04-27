package nl.lekkeratlas.worker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        classes = WorkerApplication.class,
        properties = {
                "spring.profiles.active=worker,dev",
                "spring.main.web-application-type=none",
                "spring.rabbitmq.listener.simple.auto-startup=false"
        }
)
class WorkerApplicationTests {

    @Test
    void contextLoads() {
    }
}