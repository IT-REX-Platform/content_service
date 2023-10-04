package de.unistuttgart.iste.gits.content_service.persistence.repository;

import de.unistuttgart.iste.gits.common.persistence.GitsRepository;
import de.unistuttgart.iste.gits.content_service.persistence.entity.ContentEntity;
import de.unistuttgart.iste.gits.generated.dto.SkillType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link ContentEntity}
 */
@Repository
public interface ContentRepository extends GitsRepository<ContentEntity, UUID> {

    @Query("select content from Content content where content.metadata.chapterId in (:chapterIds)")
    List<ContentEntity> findByChapterIdIn(List<UUID> chapterIds);

    @Query("select content from Content content where content.metadata.courseId in (:courseIds)")
    List<ContentEntity> findByCourseIdIn(List<UUID> courseIds);

    /**
     * database function to retrieve Content Entities by their Content IDs
     *
     * @param contentIds list of content IDs to be retrieved from the database
     * @return List of Content Entities that match the content IDs given as input
     */
    List<ContentEntity> findContentEntitiesByIdIn(List<UUID> contentIds);

    /**
     * Fetches all skill types for content in a chapter.
     *
     * @param chapterId the chapter id
     * @return a list of skill types
     */
    @Query("select assessment.assessmentMetadata.skillTypes from Assessment assessment where assessment.metadata.chapterId = :chapterId")
    List<List<SkillType>> findSkillTypesByChapterId(@Param("chapterId") UUID chapterId);
}
