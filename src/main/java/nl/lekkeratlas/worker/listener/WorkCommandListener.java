package nl.lekkeratlas.worker.listener;

import nl.lekkeratlas.shared.command.AddChannelCommand;
import nl.lekkeratlas.shared.command.AddVideoCommand;
import nl.lekkeratlas.shared.command.WorkCommandEnvelope;
import nl.lekkeratlas.shared.rabbit.RabbitNames;
import nl.lekkeratlas.worker.handler.AddChannelCommandHandler;
import nl.lekkeratlas.worker.handler.AddVideoCommandHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Generic listener for the single shared work queue.
 *
 * It receives a WorkCommandEnvelope and dispatches the payload to the correct
 * command handler based on the envelope type.
 */
@Component
public class WorkCommandListener {

    private final ObjectMapper objectMapper;
    private final AddChannelCommandHandler addChannelCommandHandler;
    private final AddVideoCommandHandler addVideoCommandHandler;

    public WorkCommandListener(
            ObjectMapper objectMapper,
            AddChannelCommandHandler addChannelCommandHandler,
            AddVideoCommandHandler addVideoCommandHandler
    ) {
        this.objectMapper = objectMapper;
        this.addChannelCommandHandler = addChannelCommandHandler;
        this.addVideoCommandHandler = addVideoCommandHandler;
    }

    @RabbitListener(queues = RabbitNames.WORK_QUEUE)
    public void handle(WorkCommandEnvelope envelope) {
        switch (envelope.type()) {
            case ADD_CHANNEL -> handleAddChannel(envelope);
            case ADD_VIDEO -> handleAddVideo(envelope);
        }
    }

    private void handleAddChannel(WorkCommandEnvelope envelope) {
        AddChannelCommand command = objectMapper.convertValue(
                envelope.payload(),
                AddChannelCommand.class
        );

        addChannelCommandHandler.handle(envelope, command);
    }

    private void handleAddVideo(WorkCommandEnvelope envelope) {
        AddVideoCommand command = objectMapper.convertValue(
                envelope.payload(),
                AddVideoCommand.class
        );

        addVideoCommandHandler.handle(envelope, command);
    }
}