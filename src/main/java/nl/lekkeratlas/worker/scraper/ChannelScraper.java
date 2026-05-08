package nl.lekkeratlas.worker.scraper;

import com.github.davidauk.youtubescraper.client.YoutubeClient;
import com.github.davidauk.youtubescraper.model.ChannelId;
import com.github.davidauk.youtubescraper.model.ChannelOverviewResponse;
import com.github.davidauk.youtubescraper.model.ChannelRequest;
import com.github.davidauk.youtubescraper.model.ChannelSort;
import com.github.davidauk.youtubescraper.model.content.ContentType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

/**
 * Temporary placeholder scraper.
 * <p>
 * Replace this with your YouTube channel scraping logic.
 */
@Component
public class ChannelScraper {

        private final YoutubeClient client;

        public ChannelScraper(YoutubeClient client) {
                this.client = client;
        }

        public ChannelOverviewResponse findVideoIds(String channelId) throws IOException, InterruptedException {

                ChannelId channel = new ChannelId(channelId);

                // The third argument can be a channel ID, username, or handle (e.g. "@LinusTechTips")
                return client.getChannel(channel, new ChannelRequest(
                        null,
                        Duration.ofSeconds(1),
                        null,
                        ChannelSort.NEWEST,
                        ContentType.VIDEOS
                ));
        }
}