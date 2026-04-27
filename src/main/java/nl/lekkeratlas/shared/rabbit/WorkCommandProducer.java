package nl.lekkeratlas.shared.rabbit;

import nl.lekkeratlas.shared.command.WorkCommandEnvelope;
import nl.lekkeratlas.shared.command.WorkCommandType;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class WorkCommandProducer {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public WorkCommandProducer(
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public UUID publish(
            WorkCommandType type,
            Object payload,
            UUID parentCommandId
    ) {
        UUID commandId = UUID.randomUUID();

        @SuppressWarnings("unchecked")
        Map<String, Object> payloadMap = objectMapper.convertValue(
                payload,
                Map.class
        );

        WorkCommandEnvelope envelope = new WorkCommandEnvelope(
                commandId,
                parentCommandId,
                type,
                payloadMap,
                Instant.now()
        );

        rabbitTemplate.convertAndSend(
                RabbitNames.WORK_EXCHANGE,
                RabbitNames.WORK_ROUTING_KEY,
                envelope
        );

        return commandId;
    }
}