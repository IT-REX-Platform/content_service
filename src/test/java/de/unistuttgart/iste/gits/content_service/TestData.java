package de.unistuttgart.iste.gits.content_service;

import de.unistuttgart.iste.gits.content_service.persistence.dao.AssessmentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.dao.AssessmentMetadataEmbeddable;
import de.unistuttgart.iste.gits.content_service.persistence.dao.ContentMetadataEmbeddable;
import de.unistuttgart.iste.gits.content_service.persistence.dao.MediaContentEntity;
import de.unistuttgart.iste.gits.generated.dto.ContentType;
import de.unistuttgart.iste.gits.generated.dto.SkillType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.HashSet;
import java.util.UUID;

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
}
