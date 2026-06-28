set shell := ["bash", "-euo", "pipefail", "-c"]

deps_dir := "dependencies"
deps_env := deps_dir + "/.env"
deps_env_template := deps_dir + "/.env.template"
deps_compose := deps_dir + "/docker-compose.yml"

default:
    @just --list

# Create dependencies/.env from the template if it does not exist yet
deps-init:
    @if [ ! -f "{{deps_env}}" ]; then \
        cp "{{deps_env_template}}" "{{deps_env}}"; \
        echo "Created {{deps_env}}"; \
        echo "Review it before running the app."; \
    else \
        echo "{{deps_env}} already exists"; \
    fi
    docker compose -f "{{deps_compose}}" pull

# Start dependencies
deps: deps-init
    docker compose --env-file "{{deps_env}}" -f "{{deps_compose}}" up -d --build --remove-orphans

# Stop dependencies
deps-down:
    docker compose --env-file "{{deps_env}}" -f "{{deps_compose}}" down

# Stop dependencies and delete local volumes
deps-reset:
    docker compose --env-file "{{deps_env}}" -f "{{deps_compose}}" down -v

# Follow dependency logs
deps-logs:
    docker compose --env-file "{{deps_env}}" -f "{{deps_compose}}" logs -f --tail=150

# Run the main backend Spring Boot app without building a jar
backend: deps
    set -a; source "{{deps_env}}"; set +a; ./gradlew bootBackend

# Run the queue listener Spring Boot app without building a jar
worker: deps
    set -a; source "{{deps_env}}"; set +a; ./gradlew bootWorker

test:
    ./gradlew test

build:
    ./gradlew clean build
