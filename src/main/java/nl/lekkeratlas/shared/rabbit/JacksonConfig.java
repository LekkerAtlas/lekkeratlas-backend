package nl.lekkeratlas.shared.rabbit;

import tools.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
    /**
     * Provides a globally configured ObjectMapper bean for use across the shared package.
     * This bean is required by components like WorkCommandProducer.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}