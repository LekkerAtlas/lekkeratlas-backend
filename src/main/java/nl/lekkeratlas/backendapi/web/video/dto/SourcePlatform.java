package nl.lekkeratlas.backendapi.web.video.dto;

import java.util.Objects;

import nl.lekkeratlas.shared.model.content.creator.CreatorAccountKind;

public enum SourcePlatform {
        YOUTUBE;

        public static SourcePlatform fromCreatorAccountKind(CreatorAccountKind accountKind) {
                Objects.requireNonNull(accountKind, "accountKind");

                return switch (accountKind) {
                        case YOUTUBE_CHANNEL -> YOUTUBE;
                };
        }
}
