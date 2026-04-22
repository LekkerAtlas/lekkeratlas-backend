package nl.lekkeratlas.lekkeratlasbackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "AUTHENTIK_HOST=http://localhost:9000",
        "AUTHENTIK_CLIENT_ID=test-client-id",
        "AUTHENTIK_CLIENT_SECRET=test-client-secret",
        "spring.security.oauth2.client.provider.authentik.authorization-uri=http://localhost:9000/application/o/authorize/",
        "spring.security.oauth2.client.provider.authentik.token-uri=http://localhost:9000/application/o/token/",
        "spring.security.oauth2.client.provider.authentik.user-info-uri=http://localhost:9000/application/o/userinfo/",
        "spring.security.oauth2.client.provider.authentik.user-name-attribute=sub",
        "spring.security.oauth2.client.provider.authentik.jwk-set-uri=http://localhost:9000/application/o/lekker-atlas/jwks/",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9000/application/o/lekker-atlas/jwks/"
})
class LekkeratlasBackendApplicationTests {

	@Test
	void contextLoads() {
	}
}