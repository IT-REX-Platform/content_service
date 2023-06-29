package de.unistuttgart.iste.gits.content_service.service;

import de.unistuttgart.iste.gits.content_service.persistence.dao.ContentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.dao.TagEntity;
import de.unistuttgart.iste.gits.content_service.persistence.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    public void synchronizeTags(ContentEntity content, List<String> tagNames) {
        List<TagEntity> currentTags = tagRepository.findByContentId(content.getId());
        List<TagEntity> existingTagsWithGivenNames = tagRepository.findByNameIn(tagNames);
        TagSynchronizationResult result = prepareSynchronization(tagNames, currentTags, existingTagsWithGivenNames);
        synchronizeWithDatabase(content, result);
    }

    void synchronizeWithDatabase(ContentEntity content, TagSynchronizationResult tagSynchronizationResult) {
        // the prepareSynchronization created TagEntity instances for new tags but did not store them --> save them to the DB
        Set<TagEntity> tagsToAdd = tagSynchronizationResult.newTagsToAdd().stream().map(tagRepository::save).collect(Collectors.toSet());
        // the newly created tags and the existing tags which are not already assigned to the content need to be linked with the content
        // for n:m relationships the links have to be created on both sides, i.e. content -> tag and tag --> content
        tagsToAdd.addAll(tagSynchronizationResult.existingTagsToAdd());
        if (content.getMetadata().getTags() == null) {
            content.getMetadata().setTags(tagsToAdd);
        } else {
            content.getMetadata().getTags().addAll(tagsToAdd);
        }
        for (TagEntity tag : tagsToAdd) {
            if (tag.getContents() == null) {
                tag.setContents(new HashSet<>(Set.of(content)));
            } else {
                tag.getContents().add(content);
            }
        }
        // Remove links between content and tag
        // Note: The tag entity is not deleted from the DB even it is not referenced by any content
        for (TagEntity tagEntity : tagSynchronizationResult.existingTagsToRemove()) {
            content.removeFromTags(tagEntity);
            tagEntity.removeFromContents(content);
        }
    }

    TagSynchronizationResult prepareSynchronization(List<String> tagNames,
                                                    List<TagEntity> currentTags,
                                                    List<TagEntity> existingTagsWithGivenNames) {
        Set<String> tagNamesSet = new HashSet<>(tagNames);
        Set<String> currentTagNames = currentTags.stream().map(TagEntity::getName).collect(Collectors.toSet());
        Set<String> tagNamesToAdd = tagNamesSet.stream().filter(name -> !currentTagNames.contains(name)).collect(Collectors.toSet());
        List<TagEntity> existingTagsToAdd = existingTagsWithGivenNames.stream().filter(tag -> tagNamesToAdd.contains(tag.getName())).toList();
        Set<String> existingTagNames = existingTagsToAdd.stream().map(TagEntity::getName).collect(Collectors.toSet());
        Set<String> tagNamesToCreate = tagNamesToAdd.stream().filter(name -> !existingTagNames.contains(name)).collect(Collectors.toSet());
        List<TagEntity> newTags = tagNamesToCreate.stream().map(TagEntity::fromName).toList();
        Set<String> tagNamesToRemove = currentTagNames.stream().filter(name -> !tagNamesSet.contains(name)).collect(Collectors.toSet());
        List<TagEntity> tagsToRemove = currentTags.stream().filter(tag -> tagNamesToRemove.contains(tag.getName())).toList();
        return new TagSynchronizationResult(existingTagsToAdd, newTags, tagsToRemove);
    }

    public record TagSynchronizationResult(Collection<TagEntity> existingTagsToAdd,
                                           Collection<TagEntity> newTagsToAdd,
                                           Collection<TagEntity> existingTagsToRemove) {
    }
}
