package nl.lekkeratlas.backendapi.web.dto;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import nl.lekkeratlas.shared.model.queue.QueueJobStatus;

public record Progress(
                @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id,
                @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) QueueJobStatus latestStatus,
                @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<ProgressStatusEvent> events,
                @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Progress> childProgresses) {
        public Progress {
                Objects.requireNonNull(id, "id");
                Objects.requireNonNull(latestStatus, "latestStatus");
                events = List.copyOf(Objects.requireNonNull(events, "events"));
                childProgresses = List.copyOf(Objects.requireNonNull(childProgresses, "childProgresses"));
        }
}
