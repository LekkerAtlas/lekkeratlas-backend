# LekkerAtlas Backend

Spring Boot backend for LekkerAtlas.

The backend consists of two Spring Boot applications:

- the API backend
- the worker / queue listener

The API handles HTTP requests and publishes asynchronous work to RabbitMQ. The worker consumes those messages and performs longer-running jobs, such as fetching channel or video metadata.

## Local development

For local development, use the `justfile` in the repository root.

The local dependency stack is defined in:

```text
dependencies/
├── .env.template
└── docker-compose.yml
```

The stack starts:

- PostgreSQL for the application database
- RabbitMQ for asynchronous commands
- Authentik PostgreSQL
- Authentik server
- Authentik worker

See the full local setup guide:

[docs/local-development.md](/docs/local-development.md)
