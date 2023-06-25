package de.unistuttgart.iste.gits.content_service.persistence.dao;

import de.unistuttgart.iste.gits.generated.dto.ContentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Embeddable
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContentMetadataEmbeddable {

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private OffsetDateTime suggestedDate;

    @Column(nullable = false)
    private int rewardPoints;

    @Column(nullable = false, name = "content_type")
    @Enumerated(EnumType.STRING)
    private ContentType type;

    @EqualsAndHashCode.Exclude
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "content_tags",
            joinColumns = @JoinColumn(name = "content_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<TagEntity> tags;

    @Column(nullable = false, name = "chapter_id")
    private UUID chapterId;
}
