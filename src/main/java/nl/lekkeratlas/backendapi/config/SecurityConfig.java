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
    @Value("${app.authentik.issuer-uri}")
    private String issuerUri;

    @Value("${app.authentik.host}")
    private String authentikHost;

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

            components.addSecuritySchemes("oauth2", new SecurityScheme()
                    .type(SecurityScheme.Type.OAUTH2)
                    .flows(new OAuthFlows()
                            .authorizationCode(new OAuthFlow()
                                    .authorizationUrl(authentikHost + "/application/o/authorize/") // TODO Improve
                                    .tokenUrl(authentikHost + "/application/o/token/") // TODO Improve
                                    .scopes(new Scopes()
                                            .addString("openid", "OpenID Connect") // TODO Improve
                                            .addString("profile", "User profile") // TODO Improve
                                            .addString("email", "Email") // TODO Improve
                                    )
                            )
                    )
            );

            openApi.components(components);
            openApi.getPaths().forEach((path, pathItem) -> {
                if (path.startsWith("/api/")) {
                    pathItem.readOperations().forEach(operation ->
                            operation.addSecurityItem(
                                    new SecurityRequirement().addList("oauth2")
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
                                - Uses Authorization Code Flow with PKCE.
                                - Public clients do NOT need a client secret.
                                - Obtain an access token via your identity provider.
                                - Call the API with: Authorization: Bearer <access_token>
                                """));
    }
}