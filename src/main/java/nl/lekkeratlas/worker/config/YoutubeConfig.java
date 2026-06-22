package nl.lekkeratlas.worker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.davidauk.youtubescraper.client.YoutubeClient;

@Configuration
public class YoutubeConfig {

        @Bean
        public YoutubeClient youtubeClient() {
                return new YoutubeClient();
        }
}
