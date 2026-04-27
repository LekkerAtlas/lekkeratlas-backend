package nl.lekkeratlas.shared.command;

import java.util.UUID;

public record AddChannelCommand(
        String channelId,
        UUID requestedByUserId
) {
}