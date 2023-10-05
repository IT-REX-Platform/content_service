package de.unistuttgart.iste.gits.content_service.controller;


import de.unistuttgart.iste.gits.common.event.ChapterChangeEvent;
import de.unistuttgart.iste.gits.common.event.ContentProgressedEvent;
import de.unistuttgart.iste.gits.content_service.service.*;
import io.dapr.Topic;
import io.dapr.client.domain.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
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

    /**
     * Listens to the content-progressed topic and logs the user progress.
     */
    @Topic(name = "content-progressed", pubsubName = "gits")
    @PostMapping(path = "/content-progressed-pubsub")
    public Mono<Void> logUserProgress(@RequestBody final CloudEvent<ContentProgressedEvent> cloudEvent) {
        return Mono.fromRunnable(() -> {
            try {
                userProgressDataService.logUserProgress(cloudEvent.getData());
            } catch (final Exception e) {
                log.error("Error while processing logUserProgress event. {}", e.getMessage());
            }
        });
    }

    @Topic(name = "chapter-changed", pubsubName = "gits")
    @PostMapping(path = "/content-service/chapter-changed-pubsub")
    public Mono<Void> cascadeCourseDeletion(@RequestBody final CloudEvent<ChapterChangeEvent> cloudEvent) {
        return Mono.fromRunnable(() -> {
            try {
                // Delete content associated with the chapter
                sectionService.cascadeSectionDeletion(cloudEvent.getData());
            } catch (final Exception e) {
                log.error("Error while processing chapter-changes event. {}", e.getMessage());
            }
            try {
                // Delete section
                contentService.cascadeContentDeletion(cloudEvent.getData());
            } catch (final Exception e) {
                log.error("Error while processing chapter-changes event. {}", e.getMessage());
            }

        });

    }












}
