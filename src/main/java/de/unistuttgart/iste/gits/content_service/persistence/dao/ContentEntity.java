package de.unistuttgart.iste.gits.content_service.persistence.dao;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DiscriminatorFormula;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

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
 * There are three indexes on this table:
 * One for the chapter id, useful for the query by chapter id.
 * One for the content type, for allowing filter by content type efficiently.
 * One for both chapter id and content type, for allowing filter by content type as a subquery of chapters efficiently.
 * <p>
 * Embeddable classes are used to have the same structure as the DTOs.
 * This makes it easier to convert between DTOs and entities.
 * <p>
 */
@Entity(name = "Content")
@Table(indexes = {
        @Index(name = "idx_content_chapter_id", columnList = "chapter_id"),
        @Index(name = "idx_content_type", columnList = "content_type"),
        @Index(name = "idx_content_type_chapter_id", columnList = "content_type, chapter_id")
})
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorFormula("case when content_type = 'MEDIA' then 'MEDIA' else 'ASSESSMENT' end")
@Data
@SuperBuilder
@NoArgsConstructor
public class ContentEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Embedded
    @Builder.Default
    private ContentMetadataEmbeddable metadata = new ContentMetadataEmbeddable();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "content_id")
    @Builder.Default
    private List<UserProgressDataEntity> userProgressData = new ArrayList<>();

    public List<String> getTagNames() {
        return Optional.ofNullable(metadata.getTags())
                .map(tags -> tags.stream().map(TagEntity::getName).toList())
                .orElse(emptyList());
    }

    public ContentEntity addToTags(TagEntity tagEntity) {
        Set<TagEntity> tags = new HashSet<>(
                Optional.ofNullable(metadata.getTags()).orElse(emptySet()));
        tags.add(tagEntity);
        metadata.setTags(tags);
        return this;
    }

    public ContentEntity removeFromTags(TagEntity tagEntity) {
        Set<TagEntity> tags = Stream.ofNullable(metadata.getTags())
                .flatMap(Set::stream)
                .filter(tag -> !tag.getName().equals(tagEntity.getName()))
                .collect(Collectors.toSet());
        this.metadata.setTags(tags);
        return this;
    }

}
