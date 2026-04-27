package nl.lekkeratlas.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WorkerApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(WorkerApplication.class);
        app.setAdditionalProfiles("worker");
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }
}