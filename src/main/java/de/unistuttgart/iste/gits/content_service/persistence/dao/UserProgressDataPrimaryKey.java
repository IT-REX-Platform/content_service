package de.unistuttgart.iste.gits.content_service.persistence.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProgressDataPrimaryKey implements Serializable {

    private UUID userId;
    private UUID contentId;
}
