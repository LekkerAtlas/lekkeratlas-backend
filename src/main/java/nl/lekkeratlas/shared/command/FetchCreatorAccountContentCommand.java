package nl.lekkeratlas.shared.command;

import java.util.Objects;
import java.util.UUID;

import nl.lekkeratlas.shared.model.content.creator.CreatorAccountKind;

public record FetchCreatorAccountContentCommand(
                String externalAccountId,
                CreatorAccountKind accountKind,
                UUID requestedByUserId) implements Command {

        public FetchCreatorAccountContentCommand {
                Objects.requireNonNull(accountKind, "accountKind");
        }

        @Override
        public String correlationKey() {
                return dedupeKey();
        }

        @Override
        public String dedupeKey() {
                return "fetchcreatoraccountcontent:"
                                + accountKind
                                + ":"
                                + externalAccountId
                                + ":"
                                + requestedByUserId;
        }
}
