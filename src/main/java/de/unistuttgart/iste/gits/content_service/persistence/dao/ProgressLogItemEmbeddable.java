package de.unistuttgart.iste.gits.content_service.persistence.dao;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Embeddable
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProgressLogItemEmbeddable {

    @Column(nullable = false)
    private OffsetDateTime timestamp;

    @Column(nullable = false)
    private boolean success;

    @Column(nullable = false)
    private double correctness;

    @Column(nullable = false)
    private int hintsUsed;

    @Column(nullable = true)
    private Integer timeToComplete;
}
