package nl.lekkeratlas.backendapi.config;

import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import io.swagger.v3.oas.models.Components;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;

import java.util.Optional;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/api-docs",
                                "/api-docs/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/webjars/**",
                                "/public/**"
                        ).permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2Login(Customizer.withDefaults())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
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
                                - All /api/** routes require OAuth2.
                                - Uses Bearer JWT access tokens.
                                - Obtain a JWT access token via your identity provider.
                                - Call the API with: Authorization: Bearer <access_token>
                                """));
    }
}