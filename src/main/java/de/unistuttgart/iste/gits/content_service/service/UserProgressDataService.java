package de.unistuttgart.iste.gits.content_service.service;

import de.unistuttgart.iste.gits.common.dapr.TopicPublisher;
import de.unistuttgart.iste.gits.common.event.UserProgressUpdatedEvent;
import de.unistuttgart.iste.gits.content_service.persistence.entity.*;
import de.unistuttgart.iste.gits.content_service.persistence.mapper.UserProgressDataMapper;
import de.unistuttgart.iste.gits.content_service.persistence.repository.UserProgressDataRepository;
import de.unistuttgart.iste.gits.generated.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;

import static de.unistuttgart.iste.gits.common.util.GitsCollectionUtils.countAsInt;

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
    public UserProgressData getUserProgressData(final UUID userId, final UUID contentId) {
        final UserProgressDataEntity dbProgressData = getUserProgressDataEntity(userId, contentId);

        return userProgressDataMapper.entityToDto(dbProgressData);
    }

    /**
     * Retrieves a User Progress Object for a user, content combination from the database
     *
     * @param userId    ID of user
     * @param contentId ID of content
     * @return User Progress Entity from the database
     */
    private UserProgressDataEntity getUserProgressDataEntity(final UUID userId, final UUID contentId) {
        return userProgressDataRepository
                .findByUserIdAndContentId(userId, contentId)
                .orElseGet(() -> createInitialUserProgressData(userId, contentId));
    }

    /**
     * Creates a User Progress Entity in the Database with no initial Progress tracked
     *
     * @param userId    ID of user
     * @param contentId ID of Content
     * @return a newly initialized User Progress Entity
     */
    public UserProgressDataEntity createInitialUserProgressData(final UUID userId, final UUID contentId) {
        log.info("Creating initial user progress data for user {} and content {}", userId, contentId);
        final ContentEntity contentEntity = contentService.requireContentExisting(contentId);

        final Integer learningInterval = contentEntity instanceof final AssessmentEntity assessmentEntity
                ? assessmentEntity.getAssessmentMetadata().getInitialLearningInterval()
                : null;

        final UserProgressDataEntity userProgressDataEntity = UserProgressDataEntity.builder()
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
     * @param userProgressUpdatedEvent the event to log
     */
    public void logUserProgress(final UserProgressUpdatedEvent userProgressUpdatedEvent) {
        final UserProgressDataEntity userProgressDataEntity = getUserProgressDataEntity(
                userProgressUpdatedEvent.getUserId(), userProgressUpdatedEvent.getContentId());

        userProgressDataEntity.setLearningInterval(
                calculateNewLearningInterval(userProgressUpdatedEvent, userProgressDataEntity));

        final var logItem = userProgressDataMapper.eventToEmbeddable(userProgressUpdatedEvent);
        logItem.setTimestamp(OffsetDateTime.now());
        userProgressDataEntity.getProgressLog().add(logItem);

        userProgressDataRepository.save(userProgressDataEntity);

        topicPublisher.notifyUserProgressUpdated(userProgressUpdatedEvent);
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
    protected Integer calculateNewLearningInterval(final UserProgressUpdatedEvent userProgressUpdatedEvent, final UserProgressDataEntity userProgressDataEntity) {
        if (userProgressDataEntity.getLearningInterval() == null) {
            return null;
        }
        final double newLearningInterval;
        if (userProgressUpdatedEvent.isSuccess()) {
            final int hintsUsedCapped = Math.min(userProgressUpdatedEvent.getHintsUsed(), 10);
            newLearningInterval = userProgressDataEntity.getLearningInterval() *
                                  (1 + userProgressUpdatedEvent.getCorrectness() - hintsUsedCapped * 0.1);
        } else {
            newLearningInterval = userProgressDataEntity.getLearningInterval()
                                  * (0.5 * userProgressUpdatedEvent.getCorrectness());
        }

        return (int) Math.floor(Math.max(1, newLearningInterval));
    }

    /**
     * Method retrieving the progress of all content within a Stage. Progress is returned as a percentage
     *
     * @param stage           Stage DTO
     * @param userId          the User progress is being tracked
     * @param requiredContent true - consider required content, false - consider optional content
     * @return progress percentage
     */
    public double getStageProgressForUser(final Stage stage, final UUID userId, final boolean requiredContent) {

        final List<Content> contentList;

        if (requiredContent) {
            contentList = stage.getRequiredContents();
        } else {
            contentList = stage.getOptionalContents();
        }

        if (contentList.isEmpty()) {
            return 100.00;
        }

        final int numbOfCompletedContent = countNumCompletedContent(userId, contentList);

        return (double) numbOfCompletedContent / contentList.size() * 100;
    }

    /**
     * Method that calculated the progress of content for an individual user for each Chapter.
     * The returned list of CompositeProgressInformation is sorted in the same order as the chapterIds list.
     *
     * @param chapterIds list of chapters for which the progress has to be evaluated
     * @param userId     the ID of the user for whom progress is evaluated
     * @return Progress for each chapter, containing a percentage of progress, absolut number of content and completed content
     */
    public List<CompositeProgressInformation> getProgressByChapterIdsForUser(final List<UUID> chapterIds, final UUID userId) {
        final List<List<Content>> contentsByChapterIds = contentService.getContentsByChapterIds(chapterIds);

        final List<CompositeProgressInformation> chapterProgressItems = new ArrayList<>();

        for (final List<Content> contentList : contentsByChapterIds) {
            final int numCompletedContent = countNumCompletedContent(userId, contentList);

            final CompositeProgressInformation compositeProgressInformation =
                    createProgressInformation(contentList, numCompletedContent);

            chapterProgressItems.add(compositeProgressInformation);
        }

        return chapterProgressItems;
    }

    private static CompositeProgressInformation createProgressInformation(final List<Content> contentList, final int numCompletedContent) {
        double progress = 100.0;

        if (!contentList.isEmpty()) {
            progress = (double) numCompletedContent / contentList.size() * 100;
        }

        return CompositeProgressInformation.builder()
                .setProgress(progress)
                .setCompletedContents(numCompletedContent)
                .setTotalContents(contentList.size())
                .build();
    }

    /**
     * function counting how many Content objects have been successfully progressed/completed
     *
     * @param userId      ID of the user progress is to be checked
     * @param contentList all content objects for which the progress has to be evaluated
     * @return number of successfully completed contents
     */
    private int countNumCompletedContent(final UUID userId, final List<Content> contentList) {

        final List<UserProgressData> userProgressDataOfContents = contentList
                .stream()
                .map(Content::getId)
                .map(contentId -> getUserProgressData(userId, contentId))
                .toList();

        return countAsInt(userProgressDataOfContents, UserProgressData::getIsLearned);
    }
}
