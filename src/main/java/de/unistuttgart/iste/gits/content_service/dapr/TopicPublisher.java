package de.unistuttgart.iste.gits.content_service.dapr;


import de.unistuttgart.iste.gits.common.event.ContentChangeEvent;
import de.unistuttgart.iste.gits.common.event.CourseAssociationEvent;
import de.unistuttgart.iste.gits.common.event.CrudOperation;
import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.content_service.persistence.entity.ContentEntity;
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
    private static final String USER_PROGRESS_UPDATED = "user-progress-updated";

    private final DaprClient client;

    /**
     * method used to publish dapr messages to a topic
     *
     * @param dto message
     */
    protected void publishEvent(final Object dto, final String topic) {
        client.publishEvent(PUBSUB_NAME, topic, dto)
                .doOnSuccess(response -> log.debug("Published message to topic {}: {}", topic, response))
                .doOnError(error -> log.error("Error while publishing message to topic {}: {}", topic, error.getMessage()))
                .subscribe();
    }

    /**
     * method to take changes done to an entity and to transmit them to the dapr topic
     *
     * @param contentEntity changed entity
     * @param operation     type of CRUD operation performed on entity
     */
    public void notifyChange(final ContentEntity contentEntity, final CrudOperation operation) {
        final CourseAssociationEvent dto = CourseAssociationEvent.builder()
                .resourceId(contentEntity.getId())
                .chapterIds(List.of(contentEntity.getMetadata().getChapterId()))
                .operation(operation)
                .build();

        publishEvent(dto, TOPIC_RESOURCE_ASSOCIATION);
    }

    public void informContentDependentServices(final List<UUID> contentEntityIds, final CrudOperation operation) {
        final ContentChangeEvent dto = ContentChangeEvent.builder()
                .contentIds(contentEntityIds)
                .operation(operation)
                .build();

        publishEvent(dto, TOPIC_CONTENT_CHANGES);
    }

    /**
     * method that takes changes done to a non-content-Entity and transmits them to the dapr topic
     *
     * @param resourceId resource that was changed
     * @param chapterIds chapters the resource is present in
     * @param operation  type of CRUD operation performed
     */
    public void forwardChange(final UUID resourceId, final List<UUID> chapterIds, final CrudOperation operation) {
        final CourseAssociationEvent dto = CourseAssociationEvent.builder()
                .resourceId(resourceId)
                .chapterIds(chapterIds)
                .operation(operation)
                .build();

        publishEvent(dto, TOPIC_RESOURCE_ASSOCIATION);
    }

    /**
     * Forwards a user progress log event to the dapr topic "user-progress-updated".
     * This differs from the topic "content-progressed" in that it is published after
     * the content service processed the event and updated the user progress data.
     *
     * @param event the event to forward
     */
    public void forwardContentProgressed(final UserProgressLogEvent event) {
        publishEvent(event, USER_PROGRESS_UPDATED);
    }

}
