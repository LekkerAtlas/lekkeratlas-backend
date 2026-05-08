package nl.lekkeratlas.backendapi.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record GetProgressResponse(
        @NotNull
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Progress progress
) {}
