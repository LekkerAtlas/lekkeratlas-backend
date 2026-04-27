package nl.lekkeratlas.worker.scraper;

import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Temporary placeholder scraper.
 *
 * Replace this with your real YouTube video metadata extraction logic.
 */
@Component
public class VideoMetadataScraper {

    public VideoMetadata scrape(String videoId) {
        // TODO: Implement real metadata scraping.
        //
        // For now this returns minimal fake metadata so the flow can be tested.
        return new VideoMetadata(
                videoId,
                null,
                null,
                null
        );
    }

    public record VideoMetadata(
            String videoUrl,
            String externalVideoId,
            String title,
            Instant publishedAt
    ) {
    }
}