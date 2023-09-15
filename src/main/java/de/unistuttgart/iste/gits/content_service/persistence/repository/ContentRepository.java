package de.unistuttgart.iste.gits.content_service.persistence.repository;

import de.unistuttgart.iste.gits.content_service.persistence.entity.ContentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link ContentEntity}
 */
@Repository
public interface ContentRepository extends JpaRepository<ContentEntity, UUID> {

    @Query("select content from Content content where content.metadata.chapterId in (:chapterIds)")
    List<ContentEntity> findByChapterIdIn(List<UUID> chapterIds);

    /**
     * database function to retrieve Content Entities by their Content IDs
     * @param contentIds list of content IDs to be retrieved from the database
     * @return List of Content Entities that match the content IDs given as input
     */
    List<ContentEntity> findContentEntitiesByIdIn(List<UUID> contentIds);
}
