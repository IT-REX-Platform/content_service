package de.unistuttgart.iste.gits.content_service.persistence.repository;

import de.unistuttgart.iste.gits.content_service.persistence.dao.UserProgressDataEntity;
import de.unistuttgart.iste.gits.content_service.persistence.dao.UserProgressDataPrimaryKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProgressDataRepository extends JpaRepository<UserProgressDataEntity, UserProgressDataPrimaryKey> {

}
