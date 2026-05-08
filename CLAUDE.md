# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 🌐 High-Level Architecture

The application follows a microservice architecture centered around a message broker (RabbitMQ) for asynchronous communication.

**1. Backend API (`backendapi`):**
*   **Role:** Handles all incoming HTTP requests from clients (e.g., web frontends). It acts as the primary ingress point.
*   **Functionality:** Exposes controllers (`ChannelController`, `VideoController`) that receive requests and translate them into asynchronous commands.
*   **Interaction:** Instead of performing heavy logic directly, it produces command messages (e.g., `AddChannelCommand`, `AddVideoCommand`) onto the RabbitMQ queue. This decouples the API from the execution complexity.
*   **Key Structure:** Contains DTOs (`AddChannelRequest`, etc.) and command envelopes (`WorkCommandEnvelope`) defining the work unit.

**2. Worker Service (`worker`):**
*   **Role:** This is the dedicated processing engine. It consumes messages from the RabbitMQ queue.
*   **Functionality:** It contains the handlers (`AddChannelCommandHandler`, `AddVideoCommandHandler`) and specialized scrapers (`ChannelScraper`, `VideoMetadataScraper`) necessary to perform the actual business logic (e.g., interacting with external APIs, scraping metadata).
*   **Interaction:** The worker is designed to be scalable, processing tasks independently based on incoming commands.

**3. Communication Layer (RabbitMQ):**
*   **Role:** Acts as the central nervous system, managing the flow of work tasks.
*   **Structure:** Defined in `shared/rabbit/` packages, setting up topologies and producers (`WorkCommandProducer`).
*   **Commands:** All work items are wrapped into a standardized `WorkCommandEnvelope` containing a specific `WorkCommandType`.

## 🛠️ Development Commands

*   **Building the Project:** Use Gradle to build the entire application:
    ```bash
    ./gradlew build
    ```
*   **Running Tests:**
    *   **All Tests:** Run all unit and integration tests using the appropriate Gradle task (e.g., `test` or `integrationTest`).
    *   **Single Test:** For isolated testing, run the specific test class (e.g., `src/test/java/nl/lekkeratlas/backendapi/web/VideoControllerTest.java`).
*   **Linting/Formatting:** (No explicit linting command is present; rely on standard Gradle quality checks.)
*   **Running Locally:** The application has two main service points:
    1.  **Backend API:** Run the `LekkeratlasBackendApplication` (e.g., using `gradlew bootRun` or similar).
    2.  **Worker:** Run the `WorkerApplication` to consume and process messages (e.g., using `gradlew worker:bootRun`).

## 🧭 File Structure Overview (High-Level)

*   **`src/main/java/nl/lekkeratlas/backendapi`**: Contains the HTTP interface and controllers.
*   **`src/main/java/nl/lekkeratlas/worker`**: Contains the message handlers, business logic, and scrapers.
*   **`src/main/java/nl/lekkeratlas/shared`**: Houses common code, notably the messaging definitions for RabbitMQ (`command` and `rabbit` packages).
*   **`src/test/java`**: Contains all unit and integration test suites.