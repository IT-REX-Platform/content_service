package de.unistuttgart.iste.gits.content_service.persistence.entity;

import de.unistuttgart.iste.gits.generated.dto.ContentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.*;

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

    @ElementCollection
    @Builder.Default
    private Set<String> tags = new HashSet<>();

    @Column(nullable = false, name = "chapter_id")
    private UUID chapterId;

    public Set<String> getTags() {
        if (tags == null) {
            tags = new HashSet<>();
        }
        return tags;
    }
}
