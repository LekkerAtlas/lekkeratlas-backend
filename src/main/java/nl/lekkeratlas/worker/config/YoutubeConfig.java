package nl.lekkeratlas.worker.config;

import com.github.davidauk.youtubescraper.client.YoutubeClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class YoutubeConfig {

        @Bean
        public YoutubeClient youtubeClient() {
                return new YoutubeClient();
        }
}
