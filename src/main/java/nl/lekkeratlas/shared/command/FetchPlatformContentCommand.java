package nl.lekkeratlas.shared.command;

import java.util.UUID;

public record FetchPlatformContentCommand(
        String channelId,
        UUID requestedByUserId
) implements Command {
        @Override
        public String correlationKey() {
                return dedupeKey();
        }

        @Override
        public String dedupeKey() {
                return "addchannel:" + channelId + ":" + requestedByUserId;
        }
}