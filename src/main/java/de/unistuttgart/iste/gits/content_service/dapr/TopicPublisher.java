package de.unistuttgart.iste.gits.content_service.dapr;

import de.unistuttgart.iste.gits.common.event.ContentChange;
import de.unistuttgart.iste.gits.common.event.CourseAssociation;
import de.unistuttgart.iste.gits.common.event.CrudOperation;
import de.unistuttgart.iste.gits.content_service.persistence.dao.ContentEntity;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Component that takes care of publishing messages to a dapr Topic
 */
@Component
@Slf4j
public class TopicPublisher {

    private static final String PUBSUB_NAME = "gits";
    private static final String TOPIC_RESOURCE_ASSOCIATION = "resource-association";

    private static final String TOPIC_CONTENT_CHANGES = "content-changes";

    private final DaprClient client;

    public TopicPublisher(){
        client = new DaprClientBuilder().build();
    }

    /**
     * method used to publish dapr messages to a topic
     * @param dto message
     */
    private void publishChanges(Object dto, String topic){
        log.info("publishing message");
        client.publishEvent(
                PUBSUB_NAME,
                topic,
                dto).block();
    }

    /**
     * method to take changes done to an entity and to transmit them to the dapr topic
     * @param contentEntity changed entity
     * @param operation type of CRUD operation performed on entity
     */
    public void notifyChange(ContentEntity contentEntity, CrudOperation operation){
        CourseAssociation dto = CourseAssociation.builder()
                .resourceId(contentEntity.getId())
                .chapterIds(List.of(contentEntity.getMetadata()
                        .getChapterId()))
                .operation(operation)
                .build();

        publishChanges(dto, TOPIC_RESOURCE_ASSOCIATION);
    }

    public void informContentDependantServices(List<UUID> contentEntities, CrudOperation operation){
        ContentChange dto = ContentChange.builder()
                .contentIds(contentEntities)
                .operation(operation)
                .build();

        publishChanges(dto, TOPIC_CONTENT_CHANGES);
    }

    /**
     * method that takes changes done to a non-content-Entity and transmits them to the dapr topic
     * @param resourceId resource that was changed
     * @param chapterIds chapters the resource is present in
     * @param operation type of CRUD operation performed
     */
    public void forwardChange(UUID resourceId, List<UUID> chapterIds, CrudOperation operation){
        CourseAssociation dto = CourseAssociation.builder()
                .resourceId(resourceId)
                .chapterIds(chapterIds)
                .operation(operation)
                .build();

        publishChanges(dto, TOPIC_RESOURCE_ASSOCIATION);
    }


}
