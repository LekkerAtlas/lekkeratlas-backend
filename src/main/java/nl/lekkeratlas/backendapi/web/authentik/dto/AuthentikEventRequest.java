package nl.lekkeratlas.backendapi.web.authentik.dto;

public record AuthentikEventRequest(
                String action,
                AuthentikUser user) {
}
