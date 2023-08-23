package de.unistuttgart.iste.gits.content_service.persistence.dao;


import jakarta.persistence.*;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Entity(name = "Stage")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column
    private UUID sectionId;

    @Column(nullable = false)
    private int position;

    @OneToMany(cascade = CascadeType.PERSIST)
    Set<ContentEntity> requiredContents;

    @OneToMany(cascade = CascadeType.PERSIST)
    Set<ContentEntity> optionalContent;
}
