package nl.lekkeratlas.lekkeratlasbackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"AUTHENTIK_HOST=http://localhost:9000",
		"AUTHENTIK_CLIENT_ID=test-client-id",
		"AUTHENTIK_CLIENT_SECRET=test-client-secret",
		"spring.http.clients.imperative.factory=simple" // Is this needed?
})
class LekkeratlasBackendApplicationTests {

	@Test
	void contextLoads() {
	}
}