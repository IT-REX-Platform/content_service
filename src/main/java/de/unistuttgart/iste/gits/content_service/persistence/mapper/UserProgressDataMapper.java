package de.unistuttgart.iste.gits.content_service.persistence.mapper;

import de.unistuttgart.iste.gits.common.event.ContentProgressedEvent;
import de.unistuttgart.iste.gits.content_service.persistence.entity.ProgressLogItemEmbeddable;
import de.unistuttgart.iste.gits.content_service.persistence.entity.UserProgressDataEntity;
import de.unistuttgart.iste.gits.generated.dto.UserProgressData;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserProgressDataMapper {

    private final ModelMapper modelMapper;

    public UserProgressData entityToDto(final UserProgressDataEntity userProgressDataEntity) {
        final UserProgressData result = modelMapper.map(userProgressDataEntity, UserProgressData.class);

        final Optional<OffsetDateTime> optionalLastLearnDate = getLastLearnDate(userProgressDataEntity);
        final Optional<OffsetDateTime> optionalNextLearnDate = getNextLearnDate(userProgressDataEntity, optionalLastLearnDate);

        result.setLastLearnDate(optionalLastLearnDate.orElse(null));
        result.setNextLearnDate(optionalNextLearnDate.orElse(null));

        result.setIsLearned(isLearned(userProgressDataEntity));
        result.setIsDueForReview(isDueForReview(optionalNextLearnDate));

        return result;
    }

    public ProgressLogItemEmbeddable eventToEmbeddable(final ContentProgressedEvent contentProgressedEvent) {
        return modelMapper.map(contentProgressedEvent, ProgressLogItemEmbeddable.class);
    }

    private static Boolean isDueForReview(final Optional<OffsetDateTime> optionalNextLearnDate) {
        return optionalNextLearnDate
                .map(nextReviewDate -> nextReviewDate.isBefore(OffsetDateTime.now()))
                .orElse(false);
    }

    private static boolean isLearned(final UserProgressDataEntity userProgressDataEntity) {
        return userProgressDataEntity.getProgressLog().stream()
                .anyMatch(ProgressLogItemEmbeddable::isSuccess);
    }

    private static Optional<OffsetDateTime> getNextLearnDate(final UserProgressDataEntity userProgressDataEntity, final Optional<OffsetDateTime> optionalLastLearnDate) {
        return userProgressDataEntity.getLearningInterval() == null
                ? Optional.empty()
                : optionalLastLearnDate.map(lastLearnDate -> lastLearnDate.plusDays(userProgressDataEntity.getLearningInterval()));
    }

    private static Optional<OffsetDateTime> getLastLearnDate(final UserProgressDataEntity userProgressDataEntity) {
        return userProgressDataEntity.getProgressLog().stream()
                .filter(ProgressLogItemEmbeddable::isSuccess)
                .map(ProgressLogItemEmbeddable::getTimestamp)
                .max(OffsetDateTime::compareTo);
    }
}
