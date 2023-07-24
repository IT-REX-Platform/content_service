package de.unistuttgart.iste.gits.content_service.service;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.content_service.dapr.TopicPublisher;
import de.unistuttgart.iste.gits.content_service.persistence.dao.*;
import de.unistuttgart.iste.gits.content_service.persistence.mapper.UserProgressDataMapper;
import de.unistuttgart.iste.gits.content_service.persistence.repository.UserProgressDataRepository;
import de.unistuttgart.iste.gits.generated.dto.UserProgressData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProgressDataService {

    private final UserProgressDataRepository userProgressDataRepository;
    private final ContentService contentService;
    private final UserProgressDataMapper userProgressDataMapper;
    private final TopicPublisher topicPublisher;

    /**
     * Returns the user progress data for the given user and content.
     * If no progress data exists for the given user and content, it will be created.
     */
    public UserProgressData getUserProgressData(UUID userId, UUID contentId) {
        UserProgressDataEntity dbProgressData = getUserProgressDataEntity(userId, contentId);

        return userProgressDataMapper.entityToDto(dbProgressData);
    }

    private UserProgressDataEntity getUserProgressDataEntity(UUID userId, UUID contentId) {
        return userProgressDataRepository
                .findByUserIdAndContentId(userId, contentId)
                .orElseGet(() -> createInitialUserProgressData(userId, contentId));
    }

    public UserProgressDataEntity createInitialUserProgressData(UUID userId, UUID contentId) {
        log.info("Creating initial user progress data for user {} and content {}", userId, contentId);
        ContentEntity contentEntity = contentService.getContentById(contentId);

        Integer learningInterval = contentEntity instanceof AssessmentEntity assessmentEntity
                ? assessmentEntity.getAssessmentMetadata().getInitialLearningInterval()
                : null;

        UserProgressDataEntity userProgressDataEntity = UserProgressDataEntity.builder()
                .userId(userId)
                .contentId(contentId)
                .progressLog(new ArrayList<>(0))
                .learningInterval(learningInterval)
                .build();

        return userProgressDataRepository.save(userProgressDataEntity);
    }

    /**
     * Logs user progress according to the given event.
     * The learning interval of the user progress data entity will be updated.
     * A new progress log item will be added to the progress log.
     * The event will be forwarded to the topic "user-progress-updated".
     *
     * @param userProgressLogEvent the event to log
     */
    public void logUserProgress(UserProgressLogEvent userProgressLogEvent) {
        UserProgressDataEntity userProgressDataEntity = getUserProgressDataEntity(
                userProgressLogEvent.getUserId(), userProgressLogEvent.getContentId());

        userProgressDataEntity.setLearningInterval(
                calculateNewLearningInterval(userProgressLogEvent, userProgressDataEntity));
        var logItem = userProgressDataMapper.eventToEmbeddable(userProgressLogEvent);
        logItem.setTimestamp(OffsetDateTime.now());
        userProgressDataEntity.getProgressLog().add(logItem);

        userProgressDataRepository.save(userProgressDataEntity);

        topicPublisher.forwardContentProgressed(userProgressLogEvent);
    }

    /**
     * Updates the learning interval of the user progress data entity based on the
     * correctness and the hints used in the last progress log event.
     * <p>
     * Currently, the learning interval is calculated as follows:
     * <ul>
     *     <li>If the last progress log event was successful, the learning interval is
     *     multiplied by 1 + correctness. The correctness is a value between 0 and 1.
     *     If the correctness was 1, the learning interval is doubled.</li>
     *     <li>Each hint also reduced the additional learning interval by 10%</li>
     *     <li>If the last progress log event was not successful, the learning interval is
     *     halved.</li>
     * </ul>
     * <p>
     * The learning interval can never be smaller than 1, except when it was never scheduled for
     * repetition to begin with.
     */
    protected Integer calculateNewLearningInterval(UserProgressLogEvent userProgressLogEvent, UserProgressDataEntity userProgressDataEntity) {
        if (userProgressDataEntity.getLearningInterval() == null) {
            return null;
        }
        double newLearningInterval;
        if (userProgressLogEvent.isSuccess()) {
            int hintsUsedCapped = Math.min(userProgressLogEvent.getHintsUsed(), 10);
            newLearningInterval = userProgressDataEntity.getLearningInterval() *
                                  (1 + userProgressLogEvent.getCorrectness() - hintsUsedCapped * 0.1);
        } else {
            newLearningInterval = userProgressDataEntity.getLearningInterval()
                                  * (0.5 * userProgressLogEvent.getCorrectness());
        }

        return (int) Math.floor(Math.max(1, newLearningInterval));
    }
}
