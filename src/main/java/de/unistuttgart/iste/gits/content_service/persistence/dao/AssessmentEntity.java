package de.unistuttgart.iste.gits.content_service.persistence.dao;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity(name = "Assessment")
@DiscriminatorValue("ASSESSMENT")
@NoArgsConstructor
@SuperBuilder
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AssessmentEntity extends ContentEntity {

    @Embedded
    @Builder.Default
    private AssessmentMetadataEmbeddable assessmentMetadata = new AssessmentMetadataEmbeddable();

}
