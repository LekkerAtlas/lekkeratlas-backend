package nl.lekkeratlas.worker.handler;

import nl.lekkeratlas.shared.command.AddVideoCommand;
import nl.lekkeratlas.shared.command.WorkCommandEnvelope;
import nl.lekkeratlas.worker.scraper.VideoMetadataScraper;
import nl.lekkeratlas.worker.scraper.VideoMetadataScraper.VideoMetadata;
import nl.lekkeratlas.worker.service.VideoImportService;
import org.springframework.stereotype.Component;

/**
 * Handles video imports.
 *
 * Both directly added videos and videos discovered from channels end up here.
 */
@Component
public class AddVideoCommandHandler {

    private final VideoMetadataScraper videoMetadataScraper;
    private final VideoImportService videoImportService;

    public AddVideoCommandHandler(
            VideoMetadataScraper videoMetadataScraper,
            VideoImportService videoImportService
    ) {
        this.videoMetadataScraper = videoMetadataScraper;
        this.videoImportService = videoImportService;
    }

    public void handle(
            WorkCommandEnvelope envelope,
            AddVideoCommand command
    ) {
        VideoMetadata metadata = videoMetadataScraper.scrape(command.videoId());

        videoImportService.upsertVideo(
                metadata,
                command.requestedByUserId(),
                envelope.parentCommandId(),
                command.source()
        );
    }
}