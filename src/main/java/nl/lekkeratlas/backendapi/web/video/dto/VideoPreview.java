package nl.lekkeratlas.backendapi.web.video.dto;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import nl.lekkeratlas.shared.model.content.ContentType;

public record VideoPreview(
                @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID contentId,
                @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String title,
                @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) CreatorInfo creatorInfo,
                @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) ContentType videoType,
                @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<VideoSource> videoSources) {
        public VideoPreview {
                Objects.requireNonNull(title, "title");
                Objects.requireNonNull(creatorInfo, "creatorInfo");
                Objects.requireNonNull(videoType, "videoType");
                videoSources = List.copyOf(Objects.requireNonNull(videoSources, "videoSources"));
        }
}
