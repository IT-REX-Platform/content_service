package de.unistuttgart.iste.gits.content_service;

import de.unistuttgart.iste.gits.content_service.persistence.dao.AssessmentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.dao.AssessmentMetadataEmbeddable;
import de.unistuttgart.iste.gits.content_service.persistence.dao.ContentMetadataEmbeddable;
import de.unistuttgart.iste.gits.content_service.persistence.dao.MediaContentEntity;
import de.unistuttgart.iste.gits.generated.dto.ContentType;
import de.unistuttgart.iste.gits.generated.dto.SkillType;

import java.time.OffsetDateTime;
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
                .assessmentMetadata(AssessmentMetadataEmbeddable.builder()
                        .skillPoints(2)
                        .skillType(SkillType.REMEMBER)
                        .initialLearningInterval(1)
                        .build());
    }

    public static ContentMetadataEmbeddable.ContentMetadataEmbeddableBuilder dummyContentMetadataEmbeddableBuilder() {
        return ContentMetadataEmbeddable.builder()
                .chapterId(UUID.randomUUID())
                .name("Test Content")
                .rewardPoints(0)
                .suggestedDate(OffsetDateTime.now())
                .type(ContentType.MEDIA);
    }
}
