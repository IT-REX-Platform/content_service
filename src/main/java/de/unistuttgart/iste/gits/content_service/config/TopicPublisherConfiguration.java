package de.unistuttgart.iste.gits.content_service.config;

import de.unistuttgart.iste.gits.content_service.dapr.TopicPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TopicPublisherConfiguration {


    @Bean
    public TopicPublisher getTopicPublisher() {
        return new TopicPublisher();
    }
}
