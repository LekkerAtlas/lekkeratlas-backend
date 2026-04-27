package nl.lekkeratlas.backendapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
		"nl.lekkeratlas.backendapi",
		"nl.lekkeratlas.shared"
})
public class LekkeratlasBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(LekkeratlasBackendApplication.class, args);
	}
}