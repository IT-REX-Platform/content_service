package de.unistuttgart.iste.gits.content_service.persistence.dao;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity(name = "Content")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private int rewardPoints;

    @Column(nullable = false)
    private boolean workedOn;

    @EqualsAndHashCode.Exclude
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "content_tags",
            joinColumns = @JoinColumn(name = "content_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<TagEntity> tags;

    @Column(nullable = false)
    private UUID chapterId;

    public ContentEntity addToTags(TagEntity tagEntity) {
        if (this.tags == null) {
            this.tags = new HashSet<>();
            this.tags.add(tagEntity);
        } else {
            this.tags.add(tagEntity);
        }
        return this;
    }

    public ContentEntity removeFromTags(TagEntity tagEntity) {
        if (this.tags != null) {
            this.tags.remove(tagEntity);
        }
        return this;
    }

}
