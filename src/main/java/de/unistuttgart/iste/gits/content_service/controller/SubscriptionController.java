package de.unistuttgart.iste.gits.content_service.controller;


import de.unistuttgart.iste.gits.common.event.ChapterChangeEvent;
import de.unistuttgart.iste.gits.common.event.ResourceUpdateEvent;
import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.content_service.service.ContentService;
import de.unistuttgart.iste.gits.content_service.service.UserProgressDataService;
import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * REST Controller Class listening to a dapr Topic.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SubscriptionController {

    private final ContentService contentService;
    private final UserProgressDataService userProgressDataService;

    @Topic(name = "resource-update", pubsubName = "gits")
    @PostMapping(path = "/content-service/resource-update-pubsub")
    public Mono<Void> updateAssociation(@RequestBody(required = false) CloudEvent<ResourceUpdateEvent> cloudEvent, @RequestHeader Map<String, String> headers){

            return Mono.fromRunnable( () -> contentService.forwardResourceUpdates(cloudEvent.getData()));
    }

    /**
     * Listens to the content-progressed topic and logs the user progress.
     */
    @Topic(name = "content-progressed", pubsubName = "gits")
    @PostMapping(path = "/content-progressed-pubsub")
    public Mono<Void> logUserProgress(@RequestBody(required = false) CloudEvent<UserProgressLogEvent> cloudEvent) {
        if (cloudEvent == null) {
            return Mono.error(new IllegalArgumentException("CloudEvent is null"));
        }
        return Mono.fromRunnable(() -> userProgressDataService.logUserProgress(cloudEvent.getData()));
    }

    @Topic(name = "chapter-changes", pubsubName = "gits")
    @PostMapping(path = "/content-service/chapter-changes-pubsub")
    public Mono<Void> cascadeCourseDeletion(@RequestBody(required = false) CloudEvent<ChapterChangeEvent> cloudEvent, @RequestHeader Map<String, String> headers){

        return Mono.fromRunnable( () -> contentService.cascadeContentDeletion(cloudEvent.getData()));
    }
}
