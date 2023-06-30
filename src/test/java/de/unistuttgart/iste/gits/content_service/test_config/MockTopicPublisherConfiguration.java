package de.unistuttgart.iste.gits.content_service.test_config;

import de.unistuttgart.iste.gits.common.event.CrudOperation;
import de.unistuttgart.iste.gits.content_service.dapr.TopicPublisher;
import de.unistuttgart.iste.gits.content_service.persistence.dao.ContentEntity;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.UUID;

@TestConfiguration
public class MockTopicPublisherConfiguration {


    @Primary
    @Bean
    public TopicPublisher getTestTopicPublisher() {
        TopicPublisher mockPublisher = Mockito.mock(TopicPublisher.class);
        Mockito.doNothing().when(mockPublisher).notifyChange(Mockito.any(ContentEntity.class), Mockito.any());
        Mockito.doNothing().when(mockPublisher).forwardChange(Mockito.any(UUID.class), Mockito.any(List.class), Mockito.any(CrudOperation.class));
        Mockito.doNothing().when(mockPublisher).informContentDependentServices(Mockito.any(List.class), Mockito.any(CrudOperation.class));
        return mockPublisher;
    }
}
