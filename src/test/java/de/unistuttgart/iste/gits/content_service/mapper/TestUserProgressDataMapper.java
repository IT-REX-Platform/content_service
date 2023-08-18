package de.unistuttgart.iste.gits.content_service.mapper;

import de.unistuttgart.iste.gits.content_service.persistence.dao.ProgressLogItemEmbeddable;
import de.unistuttgart.iste.gits.content_service.persistence.dao.UserProgressDataEntity;
import de.unistuttgart.iste.gits.content_service.persistence.mapper.UserProgressDataMapper;
import de.unistuttgart.iste.gits.generated.dto.UserProgressData;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class TestUserProgressDataMapper {

    private UserProgressDataMapper userProgressDataMapper = new UserProgressDataMapper(new ModelMapper());

    @Test
    void testFullMapping() {
        UserProgressDataEntity userProgressDataEntity = UserProgressDataEntity.builder()
                .userId(UUID.randomUUID())
                .id(UUID.randomUUID())
                .contentId(UUID.randomUUID())
                .learningInterval(2)
                .progressLog(List.of(
                        ProgressLogItemEmbeddable.builder()
                                .correctness(1.0)
                                .hintsUsed(1)
                                .success(true)
                                .timestamp(OffsetDateTime.parse("2021-01-01T00:00:00Z"))
                                .build()))
                .build();

        UserProgressData actual = userProgressDataMapper.entityToDto(userProgressDataEntity);

        assertThat(actual.getUserId(), is(userProgressDataEntity.getUserId()));
        assertThat(actual.getContentId(), is(userProgressDataEntity.getContentId()));
        assertThat(actual.getLearningInterval(), is(userProgressDataEntity.getLearningInterval()));
        assertThat(actual.getLog(), contains(
                allOf(
                        hasProperty("correctness", is(1.0)),
                        hasProperty("hintsUsed", is(1)),
                        hasProperty("success", is(true)),
                        hasProperty("timestamp", is(OffsetDateTime.parse("2021-01-01T00:00:00Z")))
                )
        ));
        assertThat(actual.getLastLearnDate(), is(OffsetDateTime.parse("2021-01-01T00:00:00Z")));
        assertThat(actual.getIsLearned(), is(true));
        assertThat(actual.getNextLearnDate(), is(OffsetDateTime.parse("2021-01-03T00:00:00Z")));
        assertThat(actual.getIsDueForReview(), is(true));
    }

    @Test
    void testContentNotLearnedSuccessful() {
        UserProgressDataEntity userProgressDataEntity = UserProgressDataEntity.builder()
                .userId(UUID.randomUUID())
                .id(UUID.randomUUID())
                .contentId(UUID.randomUUID())
                .learningInterval(2)
                .progressLog(List.of(
                        ProgressLogItemEmbeddable.builder()
                                .correctness(0.0)
                                .hintsUsed(1)
                                .success(false)
                                .timestamp(OffsetDateTime.parse("2021-01-01T00:00:00Z"))
                                .build()))
                .build();

        UserProgressData actual = userProgressDataMapper.entityToDto(userProgressDataEntity);

        assertThat(actual.getLastLearnDate(), is(nullValue()));
        assertThat(actual.getIsLearned(), is(false));
        assertThat(actual.getNextLearnDate(), is(nullValue()));
        assertThat(actual.getIsDueForReview(), is(false));
    }

    @Test
    void testContentNotLearnedYet() {
        UserProgressDataEntity userProgressDataEntity = UserProgressDataEntity.builder()
                .userId(UUID.randomUUID())
                .id(UUID.randomUUID())
                .contentId(UUID.randomUUID())
                .learningInterval(2).build();

        UserProgressData actual = userProgressDataMapper.entityToDto(userProgressDataEntity);

        assertThat(actual.getLastLearnDate(), is(nullValue()));
        assertThat(actual.getIsLearned(), is(false));
        assertThat(actual.getNextLearnDate(), is(nullValue()));
        assertThat(actual.getIsDueForReview(), is(false));
    }

    @Test
    void testContentNotDueForRepetitionYet() {
        UserProgressDataEntity userProgressDataEntity = UserProgressDataEntity.builder()
                .userId(UUID.randomUUID())
                .id(UUID.randomUUID())
                .contentId(UUID.randomUUID())
                .learningInterval(2)
                .progressLog(List.of(
                        ProgressLogItemEmbeddable.builder()
                                .correctness(0.0)
                                .hintsUsed(1)
                                .success(true)
                                .timestamp(OffsetDateTime.now())
                                .build()))
                .build();

        UserProgressData actual = userProgressDataMapper.entityToDto(userProgressDataEntity);

        assertThat(actual.getLastLearnDate(), is(not(nullValue())));
        assertThat(actual.getIsLearned(), is(true));
        assertThat(actual.getNextLearnDate(), is(actual.getLastLearnDate().plusDays(2)));
        assertThat(actual.getIsDueForReview(), is(false));
    }
}
