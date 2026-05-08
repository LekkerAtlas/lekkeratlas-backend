package nl.lekkeratlas.worker.scraper;

import com.github.davidauk.youtubescraper.client.YoutubeClient;
import com.github.davidauk.youtubescraper.model.content.Video;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Temporary placeholder scraper.
 * <p>
 * Replace this with your real YouTube video metadata extraction logic.
 */
@Component
public class VideoMetadataScraper {

        private final YoutubeClient client;

        public VideoMetadataScraper(YoutubeClient client) {
                this.client = client;
        }

        public Video scrape(String videoId) throws IOException, InterruptedException {
                return client.getVideo(videoId);
        }
}