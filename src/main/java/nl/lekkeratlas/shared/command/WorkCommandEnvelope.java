package nl.lekkeratlas.shared.command;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import nl.lekkeratlas.shared.model.queue.QueueJob;
import nl.lekkeratlas.shared.model.queue.QueueJobType;

/**
 * Generic RabbitMQ command wrapper.
 *
 * The queue receives only this envelope type. The actual command-specific data
 * is stored in payload and interpreted using the type field.
 */
public record WorkCommandEnvelope(
                UUID commandId,
                UUID parentCommandId,
                QueueJobType type,
                Map<String, Serializable> payload,
                Instant requestedAt) {
        public WorkCommandEnvelope(QueueJob queueJob) {
                this(
                                queueJob.getId(),
                                queueJob.getParentJob() != null ? queueJob.getParentJob().getId() : null,
                                queueJob.getType(),
                                queueJob.getPayload(),
                                Instant.now());
        }
}
