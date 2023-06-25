package de.unistuttgart.iste.gits.content_service.config;

import de.unistuttgart.iste.gits.common.dapr.CrudOperation;
import de.unistuttgart.iste.gits.content_service.dapr.TopicPublisher;
import de.unistuttgart.iste.gits.content_service.persistence.dao.ContentEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.UUID;

@Configuration
@Profile("dev")
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
        public void notifyChange(ContentEntity contentEntity, CrudOperation operation) {
            log.info("notifyChange called with {} and {}", contentEntity, operation);
        }

        @Override
        public void forwardChange(UUID resourceId, List<UUID> chapterIds, CrudOperation operation) {
            log.info("forwardChange called with {}, {} and {}", resourceId, chapterIds, operation);
        }
    }
}
