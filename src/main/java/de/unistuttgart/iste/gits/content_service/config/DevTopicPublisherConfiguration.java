package de.unistuttgart.iste.gits.content_service.config;

import de.unistuttgart.iste.gits.content_service.dapr.TopicPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.*;

@Configuration
@Profile("!prod")
@Slf4j
public class DevTopicPublisherConfiguration {

    @Bean
    public TopicPublisher getTopicPublisher() {
        log.warn("TopicPublisher is mocked. This is intended for development use only.");
        return new MockTopicPublisher();
    }

    @Slf4j
    static class MockTopicPublisher extends TopicPublisher {

        public MockTopicPublisher() {
            super(null);
        }

        @Override
        protected void publishEvent(final Object dto, final String topic) {
            log.info("Would have published message to topic {}: {}", topic, dto);
        }
    }
}
