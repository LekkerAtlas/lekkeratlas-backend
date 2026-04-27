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

## Asynchronous Communication and Scaling

The application is designed with a decoupled, asynchronous messaging backbone using RabbitMQ. This pattern ensures that the primary API remains fast and responsive by offloading heavy or long-running tasks (like scraping video metadata or processing channels) to dedicated Worker services.

**Flow:**
1.  **Producer (API):** When a client initiates a task (e.g., adding a channel), the API does not perform the work. Instead, it creates a standardized `WorkCommandEnvelope` (e.g., `AddChannelCommand`) and **produces** this message onto a designated RabbitMQ queue.
2.  **Broker (RabbitMQ):** RabbitMQ reliably queues the message and ensures it is persisted until a consumer is available.
3.  **Consumer (Worker):** The Worker service is configured to **listen** to the queue. Upon receiving a command, it consumes the message and executes the actual business logic (e.g., calling external APIs or performing complex processing).

**Benefit:** This decoupling allows the Worker service to be scaled independently of the API. If task load increases, you can simply spin up more worker instances to process the queue, improving resilience and throughput without touching the API layer.


## Notes

- The app uses OAuth2 Login for browser authentication
- JWT validation is enabled for API endpoints
- A custom HTTP client configuration may be required locally due to HTTP (non-HTTPS) behavior with Authentik
