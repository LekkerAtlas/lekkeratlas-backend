package nl.lekkeratlas.backendapi.web.video.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record VideoSource(
                @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String id,
                @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) SourcePlatform sourcePlatform) {
}
