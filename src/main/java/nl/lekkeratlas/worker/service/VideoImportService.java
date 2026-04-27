package nl.lekkeratlas.worker.service;

import nl.lekkeratlas.shared.command.AddVideoSource;
import nl.lekkeratlas.worker.scraper.VideoMetadataScraper.VideoMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Imports/upserts video metadata into the database.
 *
 * This is intentionally a placeholder. Later this should call your repository,
 * DAO or JDBC layer and use an idempotent upsert based on platform + video ID.
 */
@Service
public class VideoImportService {

    private static final Logger logger = LoggerFactory.getLogger(VideoImportService.class);

    public void upsertVideo(
            VideoMetadata metadata,
            UUID requestedByUserId,
            UUID parentCommandId,
            AddVideoSource source
    ) {
        // TODO: Replace with database upsert.
        logger.info(
                "Would upsert video. videoId={}, externalVideoId={}, title={}, requestedByUserId={}, parentCommandId={}, source={}",
                metadata.videoUrl(),
                metadata.externalVideoId(),
                metadata.title(),
                requestedByUserId,
                parentCommandId,
                source
        );
    }
}