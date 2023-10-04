package de.unistuttgart.iste.gits.content_service.persistence.entity;

import de.unistuttgart.iste.gits.common.persistence.IWithId;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DiscriminatorFormula;

import java.util.UUID;

/**
 * Super class for assessment and media content.
 * The type is determined by the "content_type" column, which is
 * {@link de.unistuttgart.iste.gits.generated.dto.ContentType#MEDIA} for media content and
 * anything else for assessment content.
 * <p>
 * The strategy for inheritance is {@link InheritanceType#SINGLE_TABLE},
 * which means that all subclasses are stored in the same table.
 * This is done to avoid joins when querying for content.
 * <p>
 * There are two indexes on this table:
 * One for the course id, useful for the query by course id.
 * One for the chapter id, useful for the query by chapter id.
 * <p>
 * Embeddable classes are used to have the same structure as the DTOs.
 * This makes it easier to convert between DTOs and entities.
 * <p>
 */
@Entity(name = "Content")
@Table(indexes = {
        @Index(name = "idx_content_course_id", columnList = "course_id"),
        @Index(name = "idx_content_chapter_id", columnList = "chapter_id"),
})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorFormula("case when content_type = 'MEDIA' then 'MEDIA' else 'ASSESSMENT' end")
@Data
@SuperBuilder
@NoArgsConstructor
public class ContentEntity implements IWithId<UUID> {

    @Id
    @GeneratedValue
    private UUID id;

    @Embedded
    @Builder.Default
    private ContentMetadataEmbeddable metadata = new ContentMetadataEmbeddable();

}
