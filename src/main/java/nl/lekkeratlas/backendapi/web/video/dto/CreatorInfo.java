package nl.lekkeratlas.backendapi.web.video.dto;

import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record CreatorInfo(
                @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID creatorId,
                @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String displayName) {
}
