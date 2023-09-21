package de.unistuttgart.iste.gits.content_service;


import de.unistuttgart.iste.gits.content_service.persistence.entity.*;
import de.unistuttgart.iste.gits.generated.dto.ContentType;
import de.unistuttgart.iste.gits.generated.dto.SkillType;

import java.time.OffsetDateTime;
import java.util.*;


public class TestData {

    public static MediaContentEntity.MediaContentEntityBuilder<?, ?> dummyMediaContentEntityBuilder() {
        return MediaContentEntity.builder()
                .metadata(dummyContentMetadataEmbeddableBuilder().build());
    }

    public static AssessmentEntity.AssessmentEntityBuilder<?, ?> dummyAssessmentEntityBuilder() {
        return AssessmentEntity.builder()
                .metadata(dummyContentMetadataEmbeddableBuilder()
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

    public static ContentMetadataEmbeddable.ContentMetadataEmbeddableBuilder dummyContentMetadataEmbeddableBuilder() {
        return ContentMetadataEmbeddable.builder()
                .chapterId(UUID.randomUUID())
                .name("Test Content")
                .rewardPoints(0)
                .suggestedDate(OffsetDateTime.now())
                .type(ContentType.MEDIA);
    }

    public static MediaContentEntity buildContentEntity(UUID chapterId) {
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
    public static UserProgressDataEntity buildDummyUserProgressData(boolean success, UUID userId, UUID contentId) {
        ProgressLogItemEmbeddable logItem = ProgressLogItemEmbeddable.builder()
                .correctness(70.00)
                .timestamp(OffsetDateTime.now())
                .hintsUsed(0)
                .success(success)
                .timeToComplete(null)
                .build();
        UserProgressDataEntity userProgressData = UserProgressDataEntity.builder()
                .userId(userId)
                .contentId(contentId)
                .progressLog(List.of(logItem))
                .learningInterval(null)
                .build();
        return userProgressData;
    }

    public static AssessmentEntity assessmentEntityWithSkillType(UUID chapterId, SkillType... skillTypes) {
        return dummyAssessmentEntityBuilder()
                .metadata(dummyContentMetadataEmbeddableBuilder()
                        .chapterId(chapterId)
                        .type(ContentType.FLASHCARDS)
                        .build())
                .assessmentMetadata(dummyAssessmentMetadataEmbeddableBuilder()
                        .skillTypes(List.of(skillTypes))
                        .build())
                .build();
    }
}
