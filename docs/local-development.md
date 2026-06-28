# Local Development Setup

This document explains how to run the LekkerAtlas backend locally.

The local setup is intentionally simple. Developers should only need to interact
with the root `justfile`.

## Project structure

```text
justfile
dependencies/
├── .env.template
└── docker-compose.yml
```

The `dependencies/` directory contains everything needed to boot the external
services used by the backend.

## What runs locally?

The local dependency stack starts:

- PostgreSQL for the LekkerAtlas application database
- RabbitMQ for asynchronous commands
- Authentik PostgreSQL
- Authentik server
- Authentik worker

The backend itself is not started through Docker during local development.
It is started directly with Gradle so code changes are faster to test.

These services are launched using docker-compose

## Requirements

Install:

- Java 21+
- Docker
- Docker Compose
- Just

Check that they are available:

```bash
java --version
docker --version
docker compose version
just --version
```

## First-time setup

From the repository root, run:

```bash
just deps-init
```

This creates:

```text
dependencies/.env
```

from:

```text
dependencies/.env.template
```

Review `dependencies/.env` before starting the application
(all values work out of the box).

[More info about variable management](#local-environment-variables)

## Starting the local dependency stack

Run:

```bash
just deps
```

This starts the local Docker Compose stack.

To view logs:

```bash
just deps-logs
```

To stop the stack:

```bash
just deps-down
```

To stop the stack and remove local volumes:

```bash
just deps-reset
```

Use `deps-reset` when you want a completely fresh local database and Authentik state.

## Running the backend

Run:

```bash
just backend
```

This starts the API backend directly through Gradle.

The backend uses the local environment values from:

```text
dependencies/.env
```

The backend runs with the local development profiles and connects to the local
dependency stack.

The backend creates messages for the worker.

## Running the worker

Run:

```bash
just worker
```

The worker consumes messages from RabbitMQ and executes asynchronous work.

## Running everything

In order for the backend to work correctly it is advised to run the backend and
worker in parallel.

## Useful commands

List available commands:

```bash
just
```

Start dependencies only:

```bash
just deps
```

Run backend only:

```bash
just backend
```

Run worker only:

```bash
just listener
```

Follow dependency logs:

```bash
just deps-logs
```

Stop dependencies:

```bash
just deps-down
```

Reset dependencies (throw away your data):

```bash
just deps-reset
```

Run tests:

```bash
just test
```

Build the project:

```bash
just build
```

## Local environment variables

Local environment values live in:

```text
dependencies/.env
```

The template lives in:

```text
dependencies/.env.template
```

When adding a new required local environment variable, update `dependencies/.env.template`.

The `dependencies/.env` is private and yours to keep/maintain.

## Authentik

Local Authentik is started as part of the dependency stack.

Default local URLs:

```text
http://localhost:9000
https://localhost:9443
```

Default local bootstrap user:

```text
admin@example.com
```

The password is configured in:

```text
dependencies/.env
```

The backend validates JWTs against the local Authentik issuer:

```text
http://localhost:9000/application/o/lekker-atlas/
```

The frontend is responsible for OIDC login. The backend validates incoming
Bearer JWT access tokens.

## RabbitMQ

RabbitMQ is used for asynchronous communication between the API backend and the
worker.

Default local management UI:

```text
http://localhost:15672
```

Default credentials are configured in:

```text
dependencies/.env
```

Flow:

1. the API receives a request
2. the API creates a command message
3. the API publishes the command to RabbitMQ
4. the worker consumes the command
5. the worker performs the long-running task
6. progress is written back through the application database and/or queue job events

This keeps API requests fast while allowing worker processing to scale separately.

## PostgreSQL

The local application database runs on:

```text
localhost:5432
```

Database values are configured in:

```text
dependencies/.env
```
