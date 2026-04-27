package nl.lekkeratlas.worker.scraper;

import com.github.davidauk.client.YoutubeClient;
import com.github.davidauk.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Temporary placeholder scraper.
 *
 * Replace this with your YouTube channel scraping logic.
 */
@Component
public class ChannelScraper {

    private static final Logger logger = LoggerFactory.getLogger(ChannelScraper.class);

    public List<String> findVideoUrls(String channelId) throws IOException, InterruptedException {

        YoutubeClient client = new YoutubeClient();

        Channel channel = new Channel(channelId);

        logger.info("Fetching videos from channel {}", channel);

        // The third argument can be a channel ID, username, or handle (e.g. "@LinusTechTips")
        return client.getChannel(channel, new ChannelRequest(
                5,
                Duration.ofSeconds(1),
                null,
                ChannelSort.NEWEST,
                ContentType.VIDEOS
        )).stream().map(Video::id).toList();
    }
}