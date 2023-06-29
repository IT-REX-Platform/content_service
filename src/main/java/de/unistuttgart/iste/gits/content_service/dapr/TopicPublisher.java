package de.unistuttgart.iste.gits.content_service.dapr;


import de.unistuttgart.iste.gits.common.event.ContentChangeEvent;
import de.unistuttgart.iste.gits.common.event.CourseAssociationEvent;
import de.unistuttgart.iste.gits.common.event.CrudOperation;
import de.unistuttgart.iste.gits.content_service.persistence.dao.ContentEntity;
import io.dapr.client.DaprClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

/**
 * Component that takes care of publishing messages to a dapr Topic
 */
@Slf4j
@RequiredArgsConstructor
public class TopicPublisher {

    private static final String PUBSUB_NAME = "gits";
    private static final String TOPIC_RESOURCE_ASSOCIATION = "resource-association";

    private static final String TOPIC_CONTENT_CHANGES = "content-changes";

    private final DaprClient client;

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
        CourseAssociationEvent dto = CourseAssociationEvent.builder()
                .resourceId(contentEntity.getId())
                .chapterIds(List.of(contentEntity.getMetadata()
                        .getChapterId()))
                .operation(operation)
                .build();

        publishChanges(dto, TOPIC_RESOURCE_ASSOCIATION);
    }

    public void informContentDependantServices(List<UUID> contentEntityIds, CrudOperation operation){
        ContentChangeEvent dto = ContentChangeEvent.builder()
                .contentIds(contentEntityIds)
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
        CourseAssociationEvent dto = CourseAssociationEvent.builder()
                .resourceId(resourceId)
                .chapterIds(chapterIds)
                .operation(operation)
                .build();

        publishChanges(dto, TOPIC_RESOURCE_ASSOCIATION);
    }


}
