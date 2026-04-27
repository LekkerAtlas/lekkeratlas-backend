package nl.lekkeratlas.backendapi.web.dto;

import java.util.UUID;

public record CommandAcceptedResponse(
        UUID commandId
) {
}