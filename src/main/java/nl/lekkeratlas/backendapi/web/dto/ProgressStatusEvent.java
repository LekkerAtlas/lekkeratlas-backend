package nl.lekkeratlas.backendapi.web.dto;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import nl.lekkeratlas.shared.model.queue.QueueJobEvent;
import nl.lekkeratlas.shared.model.queue.QueueJobStatus;

public record ProgressStatusEvent(
                @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
                @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String message,
                @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) QueueJobStatus status) {
        public ProgressStatusEvent {
                Objects.requireNonNull(id, "id");
                Objects.requireNonNull(message, "message");
                Objects.requireNonNull(status, "status");
        }

        public ProgressStatusEvent(QueueJobEvent event) {
                this(event.getId(), event.getMessage(), event.getStatus());
        }

        public static List<ProgressStatusEvent> from(List<QueueJobEvent> events) {
                return events.stream().map(ProgressStatusEvent::new).toList();
        }
}
