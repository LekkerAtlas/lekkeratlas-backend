package nl.lekkeratlas.backendapi.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;
import java.util.Optional;

@Configuration
public class SecurityConfig {

        private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

        @Value("${app.cors.allowed-origins:http://localhost,http://localhost:5173}")
        private List<String> allowedOrigins;

        @Value("${app.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}")
        private List<String> allowedMethods;

        @Value("${app.cors.allowed-headers:Authorization,Content-Type,X-Authentik-Webhook-Secret}")
        private List<String> allowedHeaders;

        @Bean
        SecurityFilterChain authentikWebhookSecurityFilterChain(
                HttpSecurity http,
                AuthentikWebhookFilter authentikWebhookFilter
        ) {
                logger.info("Building Authentik webhook SecurityFilterChain");

                return http
                        .securityMatcher("/webhooks/authentik")
                        .csrf(csrf -> csrf
                                .ignoringRequestMatchers("/webhooks/authentik")
                        )
                        .addFilterBefore(authentikWebhookFilter, UsernamePasswordAuthenticationFilter.class)
                        .authorizeHttpRequests(auth -> auth
                                .anyRequest().permitAll()
                        )
                        .build();
        }

        @Bean
        SecurityFilterChain applicationSecurityFilterChain(HttpSecurity http) {
                return http
                        .cors(Customizer.withDefaults())
                        .csrf(AbstractHttpConfigurer::disable)
                        .authorizeHttpRequests(auth -> auth
                                .requestMatchers("/public/**", "/", "/api-docs/**", "/swagger-ui/**").permitAll()
                                .requestMatchers("/api/**").authenticated()
                                .anyRequest().denyAll()
                        )
                        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                        .build();
        }

        @Bean
        OpenApiCustomizer apiSecurityCustomizer() {
                return openApi -> {
                        Components components = Optional.ofNullable(openApi.getComponents())
                                .orElseGet(Components::new);

                        components.addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste a JWT access token from the identity provider. Use the value as: Bearer <access_token>.")
                        );

                        openApi.components(components);
                        openApi.getPaths().forEach((path, pathItem) -> {
                                if (path.startsWith("/api/")) {
                                        pathItem.readOperations().forEach(operation ->
                                                operation.addSecurityItem(
                                                        new SecurityRequirement().addList("bearerAuth")
                                                )
                                        );
                                }
                        });
                };

        }

        @Bean
        OpenAPI openAPI() {
                return new OpenAPI()
                        .info(new Info()
                                .title("Lekkeratlas API")
                                .version("v1")
                                .description("""
                                        Public API for Lekkeratlas.
                                        
                                        Authentication:
                                        - All /api/** routes require a Bearer JWT access token.
                                        - Uses Bearer JWT access tokens.
                                        - Obtain a JWT access token via your identity provider.
                                        - Call the API with: Authorization: Bearer <access_token>
                                        """));
        }
}