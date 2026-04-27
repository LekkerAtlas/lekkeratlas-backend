package nl.lekkeratlas.shared.command;

import java.util.UUID;

public record AddVideoCommand(
        String videoId,
        UUID requestedByUserId,
        AddVideoSource source
) {
}