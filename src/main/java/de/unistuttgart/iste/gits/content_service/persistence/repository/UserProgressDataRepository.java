package de.unistuttgart.iste.gits.content_service.persistence.repository;

import de.unistuttgart.iste.gits.content_service.persistence.dao.UserProgressDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserProgressDataRepository extends JpaRepository<UserProgressDataEntity, UUID> {

    Optional<UserProgressDataEntity> findByUserIdAndContentId(UUID userId, UUID contentId);

}
