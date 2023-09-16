package de.unistuttgart.iste.gits.content_service.controller;


import de.unistuttgart.iste.gits.common.event.ChapterChangeEvent;
import de.unistuttgart.iste.gits.common.event.ResourceUpdateEvent;
import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.content_service.service.ContentService;
import de.unistuttgart.iste.gits.content_service.service.SectionService;
import de.unistuttgart.iste.gits.content_service.service.UserProgressDataService;
import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST Controller Class listening to a dapr Topic.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SubscriptionController {

    private final ContentService contentService;
    private final SectionService sectionService;
    private final UserProgressDataService userProgressDataService;

    @Topic(name = "resource-update", pubsubName = "gits")
    @PostMapping(path = "/content-service/resource-update-pubsub")
    public Mono<Void> updateAssociation(@RequestBody CloudEvent<ResourceUpdateEvent> cloudEvent) {

        return Mono.fromRunnable(() -> {
            try {
                contentService.forwardResourceUpdates(cloudEvent.getData());
            } catch (Exception e) {
                log.error("Error while processing resource-update event. {}", e.getMessage());
            }
        });
    }

    /**
     * Listens to the content-progressed topic and logs the user progress.
     */
    @Topic(name = "content-progressed", pubsubName = "gits")
    @PostMapping(path = "/content-progressed-pubsub")
    public Mono<Void> logUserProgress(@RequestBody CloudEvent<UserProgressLogEvent> cloudEvent) {
        return Mono.fromRunnable(() -> {
            try {
                userProgressDataService.logUserProgress(cloudEvent.getData());
            } catch (Exception e) {
                log.error("Error while processing logUserProgress event. {}", e.getMessage());
            }
        });
    }

    @Topic(name = "chapter-changes", pubsubName = "gits")
    @PostMapping(path = "/content-service/chapter-changes-pubsub")
    public Mono<Void> cascadeCourseDeletion(@RequestBody CloudEvent<ChapterChangeEvent> cloudEvent) {
        return Mono.fromRunnable(() -> {
            try {
                // Delete content associated with the chapter
                sectionService.cascadeSectionDeletion(cloudEvent.getData());
            } catch (Exception e) {
                log.error("Error while processing chapter-changes event. {}", e.getMessage());
            }
            try {
                // Delete section
                contentService.cascadeContentDeletion(cloudEvent.getData());
            } catch (Exception e) {
                log.error("Error while processing chapter-changes event. {}", e.getMessage());
            }

        });

    }












}
