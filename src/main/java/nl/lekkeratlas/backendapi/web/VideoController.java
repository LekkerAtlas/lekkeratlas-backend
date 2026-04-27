package nl.lekkeratlas.backendapi.web;

import nl.lekkeratlas.backendapi.web.dto.AddVideoRequest;
import nl.lekkeratlas.backendapi.web.dto.CommandAcceptedResponse;
import nl.lekkeratlas.shared.command.AddVideoCommand;
import nl.lekkeratlas.shared.command.AddVideoSource;
import nl.lekkeratlas.shared.command.WorkCommandType;
import nl.lekkeratlas.shared.rabbit.WorkCommandProducer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private final WorkCommandProducer workCommandProducer;

    public VideoController(WorkCommandProducer workCommandProducer) {
        this.workCommandProducer = workCommandProducer;
    }

    @PostMapping
    public ResponseEntity<CommandAcceptedResponse> addVideo(
            @RequestBody AddVideoRequest request
    ) {
        UUID userId = resolveCurrentUserId();

        UUID commandId = workCommandProducer.publish(
                WorkCommandType.ADD_VIDEO,
                new AddVideoCommand(
                        request.videoId(),
                        userId,
                        AddVideoSource.DIRECT_USER_REQUEST
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