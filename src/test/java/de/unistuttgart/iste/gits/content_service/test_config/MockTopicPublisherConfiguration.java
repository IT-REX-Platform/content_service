package de.unistuttgart.iste.gits.content_service.test_config;

import de.unistuttgart.iste.gits.common.event.CrudOperation;
import de.unistuttgart.iste.gits.content_service.dapr.TopicPublisher;
import de.unistuttgart.iste.gits.content_service.persistence.entity.ContentEntity;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

@TestConfiguration
public class MockTopicPublisherConfiguration {


    @Primary
    @Bean
    public TopicPublisher getTestTopicPublisher() {
        TopicPublisher mockPublisher = Mockito.mock(TopicPublisher.class);
        doNothing().when(mockPublisher).notifyChange(any(ContentEntity.class), any());
        doNothing().when(mockPublisher).forwardChange(any(UUID.class), any(), any(CrudOperation.class));
        doNothing().when(mockPublisher).informContentDependentServices(any(), any(CrudOperation.class));
        return mockPublisher;
    }
}
