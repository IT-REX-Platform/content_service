package de.unistuttgart.iste.gits.content_service;


import de.unistuttgart.iste.gits.content_service.persistence.entity.*;
import de.unistuttgart.iste.gits.generated.dto.ContentType;
import de.unistuttgart.iste.gits.generated.dto.SkillType;

import java.time.OffsetDateTime;
import java.util.*;


public class TestData {

    public static MediaContentEntity.MediaContentEntityBuilder<?, ?> dummyMediaContentEntityBuilder(final UUID courseId) {
        return MediaContentEntity.builder()
                .metadata(dummyContentMetadataEmbeddableBuilder(courseId).build());
    }

    public static AssessmentEntity.AssessmentEntityBuilder<?, ?> dummyAssessmentEntityBuilder(final UUID courseId) {
        return AssessmentEntity.builder()
                .metadata(dummyContentMetadataEmbeddableBuilder(courseId)
                        .type(ContentType.FLASHCARDS)
                        .build())
                .assessmentMetadata(dummyAssessmentMetadataEmbeddableBuilder().build());
    }

    public static AssessmentMetadataEmbeddable.AssessmentMetadataEmbeddableBuilder dummyAssessmentMetadataEmbeddableBuilder() {
        return AssessmentMetadataEmbeddable.builder()
                .skillPoints(2)
                .skillTypes(List.of(SkillType.REMEMBER))
                .initialLearningInterval(1);
    }

    public static ContentMetadataEmbeddable.ContentMetadataEmbeddableBuilder dummyContentMetadataEmbeddableBuilder(final UUID courseId) {
        return ContentMetadataEmbeddable.builder()
                .courseId(courseId)
                .chapterId(UUID.randomUUID())
                .name("Test Content")
                .rewardPoints(0)
                .suggestedDate(OffsetDateTime.now())
                .type(ContentType.MEDIA);
    }

    public static MediaContentEntity buildContentEntity(final UUID chapterId) {
        return MediaContentEntity.builder()
                .id(UUID.randomUUID())
                .metadata(
                        ContentMetadataEmbeddable.builder()
                                .tags(new HashSet<>())
                                .name("Test")
                                .type(ContentType.MEDIA)
                                .suggestedDate(OffsetDateTime.parse("2020-01-01T00:00:00.000Z"))
                                .rewardPoints(20)
                                .chapterId(chapterId)
                                .courseId(UUID.randomUUID())
                                .build()
                ).build();
    }

    /**
     * helper method to generate some progress data
     *
     * @param success   if evaluation of progress is a success
     * @param userId    ID of the User this Progress data belongs to
     * @param contentId ID of the Content the Progress is tracked for
     * @return database representation of a Progress data Item
     */
    public static UserProgressDataEntity buildDummyUserProgressData(final boolean success, final UUID userId, final UUID contentId) {
        final ProgressLogItemEmbeddable logItem = ProgressLogItemEmbeddable.builder()
                .correctness(70.00)
                .timestamp(OffsetDateTime.now())
                .hintsUsed(0)
                .success(success)
                .timeToComplete(null)
                .build();
        return UserProgressDataEntity.builder()
                .userId(userId)
                .contentId(contentId)
                .progressLog(List.of(logItem))
                .learningInterval(null)
                .build();
    }

    public static AssessmentEntity assessmentEntityWithSkillType(final UUID courseId, final UUID chapterId, final SkillType... skillTypes) {
        return dummyAssessmentEntityBuilder(courseId)
                .metadata(dummyContentMetadataEmbeddableBuilder(courseId)
                        .chapterId(chapterId)
                        .type(ContentType.FLASHCARDS)
                        .build())
                .assessmentMetadata(dummyAssessmentMetadataEmbeddableBuilder()
                        .skillTypes(List.of(skillTypes))
                        .build())
                .build();
    }
}
