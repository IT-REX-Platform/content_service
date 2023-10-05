package de.unistuttgart.iste.gits.content_service.config;

import de.unistuttgart.iste.gits.common.dapr.TopicPublisher;
import io.dapr.client.DaprClientBuilder;
import org.springframework.context.annotation.*;


@Configuration
@Profile("prod")
public class TopicPublisherConfiguration {

    @Bean
    public TopicPublisher getTopicPublisher() {
        return new TopicPublisher(new DaprClientBuilder().build());
    }

}
