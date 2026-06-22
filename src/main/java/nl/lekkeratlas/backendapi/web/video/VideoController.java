package nl.lekkeratlas.backendapi.web.video;

import nl.lekkeratlas.shared.rabbit.WorkCommandProducer;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

        private final WorkCommandProducer workCommandProducer;

        public VideoController(WorkCommandProducer workCommandProducer) {
                this.workCommandProducer = workCommandProducer;
        }

        // @PostMapping
        // public ResponseEntity<CommandAcceptedResponse> addVideo(
        // @RequestBody AddVideoRequest request,
        // JwtAuthenticationToken authentication
        // ) {
        // UUID userId = Utils.resolveCurrentUserId(authentication);
        //
        // try (Connection connection = Database.getConnection()) {
        //
        // try (Dao<User, UUID> userDao = DAOFactory.createDAO(connection, User.class))
        // {
        // // TODO Validate user
        // }
        //
        // try (Dao<QueueJob, UUID> queueJobDao = DAOFactory.createDAO(connection,
        // QueueJob.class)) {
        // QueueJob queueJob = workCommandProducer.publish(
        // QueueJobType.FETCH_VIDEO_METADATA,
        // new FetchVideoMetadataCommand(
        // request.videoId(),
        // userId,
        // AddVideoSource.DIRECT_USER_REQUEST
        // ),
        // null,
        // queueJobDao
        // );
        //
        // return ResponseEntity.accepted()
        // .body(new CommandAcceptedResponse(queueJob.getId()));
        // }
        //
        // } catch (SQLException e) {
        // throw new RuntimeException(e);
        // }
}
