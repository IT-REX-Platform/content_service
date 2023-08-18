package de.unistuttgart.iste.gits.content_service.persistence.dao;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OrderBy;

import java.util.*;

@Entity(name = "UserProgressData")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProgressDataEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "content_id")
    private UUID contentId;

    @ElementCollection
    @OrderBy(clause = "timestamp DESC")
    @Builder.Default
    private List<ProgressLogItemEmbeddable> progressLog = new ArrayList<>();

    @Column(nullable = true)
    private Integer learningInterval;
}
