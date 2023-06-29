package de.unistuttgart.iste.gits.content_service.persistence.dao;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OrderBy;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity(name = "UserProgressData")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(UserProgressDataPrimaryKey.class)
public class UserProgressDataEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @Column(name = "content_id", nullable = false)
    private UUID contentId;

    @ElementCollection
    @OrderBy(clause = "timestamp DESC")
    private List<ProgressLogItemEmbeddable> progressLog;

    @Column(nullable = true)
    private Integer learningInterval;

    public Optional<OffsetDateTime> getLastLearnDate() {
        return progressLog.stream()
                .map(ProgressLogItemEmbeddable::getTimestamp)
                .max(OffsetDateTime::compareTo);
    }

    public Optional<OffsetDateTime> getNextLearnDate() {
        if (learningInterval == null) {
            return Optional.empty();
        }
        return getLastLearnDate()
                .map(lastLearnDate -> lastLearnDate.plusDays(learningInterval));
    }
}
