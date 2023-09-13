package de.unistuttgart.iste.gits.content_service.persistence.dao;

import de.unistuttgart.iste.gits.generated.dto.SkillType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @implNote fields are nullable because media content does not have them, and we use single table inheritance.
 */
@Embeddable
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AssessmentMetadataEmbeddable {

    @Column(nullable = true)
    private int skillPoints;

    @Column(nullable = true)
    private List<SkillType> skillTypes;

    @Column(nullable = true)
    private int initialLearningInterval;
}
