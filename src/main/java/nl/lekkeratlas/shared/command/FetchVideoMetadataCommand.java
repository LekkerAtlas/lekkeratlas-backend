package nl.lekkeratlas.shared.command;

import java.util.UUID;

public record FetchVideoMetadataCommand(
                String videoId,
                UUID requestedByUserId,
                UUID contentPlatformId,
                AddVideoSource source) implements Command {
        @Override
        public String correlationKey() {
                return "addvideo:" + videoId + ":" + requestedByUserId;
        }

        // TODO Update so that the same video can't be added multiple times (why would
        // we want that?)
        @Override
        public String dedupeKey() {
                return "addvideo:" + videoId + ":" + requestedByUserId;
        }
}
