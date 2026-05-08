package nl.lekkeratlas.shared.command;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public interface Command {
        UUID requestedByUserId();
        String correlationKey();
        String dedupeKey();

        default boolean isDedupeKey(String key) {
                return key.equals(dedupeKey());
        }

        default Map<String, Object> serialize() {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.convertValue(this, new TypeReference<>() {});
        }
}
