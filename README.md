# LekkerAtlas Backend

This is a Spring Boot backend that integrates with Authentik using OAuth2 / OpenID Connect (OIDC).

## Requirements

- Java 21+
- Docker (for running Authentik or the app in containers)
- An Authentik instance configured with an OAuth2 / OIDC provider

## Configuration

The application relies on environment variables for sensitive and environment-specific configuration.

### Required environment variables

```
AUTHENTIK_HOST=https://your-authentik-domain
AUTHENTIK_CLIENT_ID=your-client-id
AUTHENTIK_CLIENT_SECRET=your-client-secret
```

These values must match the configuration of your Authentik OAuth2 provider.

## Authentik setup

In Authentik, configure:

- **Provider type:** OAuth2 / OpenID Connect
- **Client type:** Confidential
- **Redirect URI:**
  ```
  http://localhost:8080/login/oauth2/code/authentik
  ```
  (adjust for production domain)
- **Scopes:** `openid`, `profile`, `email`

## Running locally

Use the `dev` profile:

```
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

This uses `application-dev.properties` for local development.

## Running in production (Docker)

The Docker container forces the `prod` profile:

```
docker build -t lekkeratlas-backend .
docker run -p 8080:8080 \
  -e AUTHENTIK_HOST=https://auth.example.com \
  -e AUTHENTIK_CLIENT_ID=... \
  -e AUTHENTIK_CLIENT_SECRET=... \
  lekkeratlas-backend
```

## Authentication flow

1. User accesses a protected endpoint
2. Spring redirects to Authentik
3. User logs in
4. Authentik redirects back with an authorization code
5. Spring exchanges the code for tokens
6. User is authenticated in the application

## Notes

- The app uses OAuth2 Login for browser authentication
- JWT validation is enabled for API endpoints
- A custom HTTP client configuration may be required locally due to HTTP (non-HTTPS) behavior with Authentik
