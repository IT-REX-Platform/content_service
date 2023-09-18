package de.unistuttgart.iste.gits.content_service.persistence.repository;

import de.unistuttgart.iste.gits.content_service.persistence.entity.TagEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TagRepository extends JpaRepository<TagEntity, UUID> {

    // sql query to delete all tags that are not used in any content
    @Modifying
    @Query("DELETE FROM Tag tag WHERE tag.name NOT IN (SELECT tag.name FROM Content content JOIN content.metadata.tags tag)")
    void deleteUnusedTags();

    // sql query to insert a tag if it does not exist
    @Modifying
    @Query(value = "INSERT INTO Tag (name) VALUES (:tagName) ON CONFLICT DO NOTHING", nativeQuery = true)
    void insertIfNotExists(String tagName);
}
