package nl.lekkeratlas.shared.command;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Generic RabbitMQ command wrapper.
 *
 * The queue receives only this envelope type. The actual command-specific data
 * is stored in payload and interpreted using the type field.
 */
public record WorkCommandEnvelope(
        UUID commandId,
        UUID parentCommandId,
        WorkCommandType type,
        Map<String, Object> payload,
        Instant requestedAt
) {
}