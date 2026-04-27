package nl.lekkeratlas.backendapi.web;

import nl.lekkeratlas.backendapi.web.dto.AddChannelRequest;
import nl.lekkeratlas.backendapi.web.dto.CommandAcceptedResponse;
import nl.lekkeratlas.shared.command.AddChannelCommand;
import nl.lekkeratlas.shared.command.WorkCommandType;
import nl.lekkeratlas.shared.rabbit.WorkCommandProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/channels")
public class ChannelController {

    private final WorkCommandProducer workCommandProducer;

    private static final Logger logger = LoggerFactory.getLogger(ChannelController.class);

    public ChannelController(WorkCommandProducer workCommandProducer) {
        this.workCommandProducer = workCommandProducer;
    }

    @PostMapping
    public ResponseEntity<CommandAcceptedResponse> addChannel(
            @RequestBody AddChannelRequest request
    ) {
        UUID userId = resolveCurrentUserId();
        System.out.println("test!");
        logger.info("--- [DEBUG: ChannelController] Publishing AddChannelCommand for URL: {} ---", request.channelUrl());

        UUID commandId = workCommandProducer.publish(
                WorkCommandType.ADD_CHANNEL,
                new AddChannelCommand(
                        request.channelUrl(),
                        userId
                ),
                null
        );

        return ResponseEntity.accepted()
                .body(new CommandAcceptedResponse(commandId));
    }

    /**
     * Temporary placeholder.
     *
     * Later this should come from Spring Security / Authentik claims.
     */
    private UUID resolveCurrentUserId() {
        return UUID.randomUUID();
    }
}