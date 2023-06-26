package de.unistuttgart.iste.gits.content_service.service;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.content_service.TestData;
import de.unistuttgart.iste.gits.content_service.persistence.dao.AssessmentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.dao.ProgressLogItemEmbeddable;
import de.unistuttgart.iste.gits.content_service.persistence.dao.UserProgressDataEntity;
import de.unistuttgart.iste.gits.content_service.persistence.mapper.UserProgressDataMapper;
import de.unistuttgart.iste.gits.content_service.persistence.repository.UserProgressDataRepository;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserProgressDataServiceTest {

    private final UserProgressDataRepository userProgressDataRepository = mock(UserProgressDataRepository.class);
    private final ContentService contentService = mock(ContentService.class);

    private final UserProgressDataService userProgressDataService = new UserProgressDataService(userProgressDataRepository,
            contentService, new UserProgressDataMapper(new ModelMapper()));

    /**
     * Given progress data exists for the user and content
     * When getUserProgressData is called
     * Then the progress data is returned
     */
    @Test
    void getUserProgressData() {
        var userProgressEntity = UserProgressDataEntity.builder()
                .progressLog(Collections.emptyList())
                .learningInterval(1)
                .userId(UUID.randomUUID())
                .contentId(UUID.randomUUID())
                .build();

        doReturn(Optional.of(userProgressEntity)).when(userProgressDataRepository).findById(any());

        var actual = userProgressDataService
                .getUserProgressData(userProgressEntity.getUserId(), userProgressEntity.getContentId());

        assertThat(actual.getUserId(), is(equalTo(userProgressEntity.getUserId())));
        assertThat(actual.getContentId(), is(equalTo(userProgressEntity.getContentId())));
        assertThat(actual.getLearningInterval(), is(equalTo(userProgressEntity.getLearningInterval())));
        assertThat(actual.getLastLearnDate(), is(nullValue()));
        assertThat(actual.getNextLearnDate(), is(nullValue()));
        assertThat(actual.getLog(), is(empty()));

        verify(userProgressDataRepository).findById(any());
    }

    /**
     * Given progress data exists for the user and content
     * When getUserProgressData is called
     * Then the progress data is returned with the correct last and next learn date
     */
    @Test
    void lastLearnDateAndNextLearnDate() {
        var userProgressEntity = UserProgressDataEntity.builder()
                .progressLog(List.of(
                        ProgressLogItemEmbeddable.builder()
                                .timestamp(OffsetDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))
                                .timeToComplete(100)
                                .hintsUsed(0)
                                .correctness(1)
                                .success(true)
                                .build()
                ))
                .learningInterval(7)
                .userId(UUID.randomUUID())
                .contentId(UUID.randomUUID())
                .build();

        doReturn(Optional.of(userProgressEntity)).when(userProgressDataRepository).findById(any());

        var actual = userProgressDataService
                .getUserProgressData(userProgressEntity.getUserId(), userProgressEntity.getContentId());

        assertThat(actual.getUserId(), is(equalTo(userProgressEntity.getUserId())));
        assertThat(actual.getContentId(), is(equalTo(userProgressEntity.getContentId())));
        assertThat(actual.getLearningInterval(), is(equalTo(userProgressEntity.getLearningInterval())));

        var expectedLastLearnDate = LocalDate.of(2021, 1, 1).atStartOfDay().atOffset(ZoneOffset.UTC);
        var expectedNextLearnDate = expectedLastLearnDate.plusDays(7);
        assertThat(actual.getLastLearnDate(), is(expectedLastLearnDate));
        assertThat(actual.getNextLearnDate(), is(expectedNextLearnDate));
        assertThat(actual.getLog(), hasSize(1));

        verify(userProgressDataRepository).findById(any());
    }

    @Test
    void userDataIsInitializedWhenAbsent() {
        var contentId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        AssessmentEntity assessmentEntity = TestData.dummyAssessmentEntityBuilder()
                .assessmentMetadata(TestData.dummyAssessmentMetadataEmbeddableBuilder()
                        .initialLearningInterval(2)
                        .build())
                .id(contentId).build();

        doReturn(Optional.empty()).when(userProgressDataRepository).findById(any());
        doReturn(assessmentEntity).when(contentService).getContentById(any());
        // save method returns its argument
        doAnswer(returnsFirstArg()).when(userProgressDataRepository).save(any(UserProgressDataEntity.class));

        var actual = userProgressDataService.getUserProgressData(userId, contentId);

        assertThat(actual.getUserId(), is(equalTo(userId)));
        assertThat(actual.getContentId(), is(equalTo(contentId)));
        assertThat(actual.getLearningInterval(), is(2));
        assertThat(actual.getLastLearnDate(), is(nullValue()));
        assertThat(actual.getNextLearnDate(), is(nullValue()));
        assertThat(actual.getLog(), is(empty()));

        verify(userProgressDataRepository).findById(any());
        verify(userProgressDataRepository, times(1)).save(any(UserProgressDataEntity.class));
    }

    /**
     * Given a user progress event
     * When logUserProgress is called
     * Then the event is added to the progress log
     */
    @Test
    void logProgress() {
        var contentId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        UserProgressLogEvent event = UserProgressLogEvent.builder()
                .contentId(contentId)
                .userId(userId)
                .timeToComplete(100)
                .correctness(1.0)
                .hintsUsed(0)
                .success(true)
                .build();

        UserProgressDataEntity initialProgress = UserProgressDataEntity.builder()
                .progressLog(new ArrayList<>())
                .learningInterval(null)
                .userId(userId)
                .contentId(contentId)
                .build();

        doReturn(Optional.of(initialProgress)).when(userProgressDataRepository).findById(any());
        doAnswer(returnsFirstArg()).when(userProgressDataRepository).save(any(UserProgressDataEntity.class));

        userProgressDataService.logUserProgress(event);

        verify(userProgressDataRepository).save(
                UserProgressDataEntity.builder()
                        .contentId(contentId)
                        .userId(userId)
                        .learningInterval(null)
                        .progressLog(List.of(
                                ProgressLogItemEmbeddable.builder()
                                        .timestamp(notNull())
                                        .timeToComplete(100)
                                        .correctness(1.0)
                                        .hintsUsed(0)
                                        .success(true)
                                        .build()))
                        .build()
        );
    }

    /**
     * Given a user progress event with 1.0 correctness and success
     * When calculateNewLearningInterval is called
     * Then the learning interval is doubled
     */
    @Test
    void learningIntervalSuccess() {
        UserProgressLogEvent userProgressLogEvent = UserProgressLogEvent.builder()
                .correctness(1.0)
                .success(true)
                .hintsUsed(0)
                .build();

        UserProgressDataEntity userProgressDataEntity = UserProgressDataEntity.builder()
                .learningInterval(2)
                .build();

        var actual = userProgressDataService.calculateNewLearningInterval(userProgressLogEvent, userProgressDataEntity);
        assertThat(actual, is(4));
    }

    /**
     * Given a user progress event with 1.0 correctness and failure
     * When calculateNewLearningInterval is called
     * Then the learning interval is halved
     */
    @Test
    void learningIntervalFailure() {
        UserProgressLogEvent userProgressLogEvent = UserProgressLogEvent.builder()
                .correctness(1.0)
                .success(false)
                .hintsUsed(0)
                .build();

        UserProgressDataEntity userProgressDataEntity = UserProgressDataEntity.builder()
                .learningInterval(10)
                .build();

        var actual = userProgressDataService.calculateNewLearningInterval(userProgressLogEvent, userProgressDataEntity);
        assertThat(actual, is(5));
    }

    /**
     * Given a user progress event with 0.1 correctness and failure
     * When calculateNewLearningInterval is called
     * Then the learning interval is reduced by 95%
     */
    @Test
    void learningIntervalFailureLowCorrectness() {
        UserProgressLogEvent userProgressLogEvent = UserProgressLogEvent.builder()
                .correctness(0.1)
                .success(false)
                .hintsUsed(0)
                .build();

        UserProgressDataEntity userProgressDataEntity = UserProgressDataEntity.builder()
                .learningInterval(100)
                .build();

        var actual = userProgressDataService.calculateNewLearningInterval(userProgressLogEvent, userProgressDataEntity);
        assertThat(actual, is(5));
    }

    /**
     * Given a user progress event with 0.5 correctness and success
     * When calculateNewLearningInterval is called
     * Then the learning interval is increased by 50%
     */
    @Test
    void learningIntervalSuccessLowCorrectness() {
        UserProgressLogEvent userProgressLogEvent = UserProgressLogEvent.builder()
                .correctness(0.5)
                .success(true)
                .hintsUsed(0)
                .build();

        UserProgressDataEntity userProgressDataEntity = UserProgressDataEntity.builder()
                .learningInterval(10)
                .build();

        var actual = userProgressDataService.calculateNewLearningInterval(userProgressLogEvent, userProgressDataEntity);
        assertThat(actual, is(15));
    }

    /**
     * Given a user progress event with 1.0 correctness and success but 1 hint used
     * When calculateNewLearningInterval is called
     * Then the learning interval is increased by 90%
     */
    @Test
    void learningIntervalHintsUsed() {
        UserProgressLogEvent userProgressLogEvent = UserProgressLogEvent.builder()
                .correctness(1.0)
                .success(true)
                .hintsUsed(1)
                .build();

        UserProgressDataEntity userProgressDataEntity = UserProgressDataEntity.builder()
                .learningInterval(10)
                .build();

        var actual = userProgressDataService.calculateNewLearningInterval(userProgressLogEvent, userProgressDataEntity);
        assertThat(actual, is(19));
    }

    /**
     * Given a user progress event with 1.0 correctness and success but 100 hints used
     * When calculateNewLearningInterval is called
     * Then the learning interval is not increased
     */
    @Test
    void learningIntervalManyHintsUsed() {
        UserProgressLogEvent userProgressLogEvent = UserProgressLogEvent.builder()
                .correctness(1.0)
                .success(true)
                .hintsUsed(100)
                .build();

        UserProgressDataEntity userProgressDataEntity = UserProgressDataEntity.builder()
                .learningInterval(10)
                .build();

        var actual = userProgressDataService.calculateNewLearningInterval(userProgressLogEvent, userProgressDataEntity);
        assertThat(actual, is(10));
    }
}
