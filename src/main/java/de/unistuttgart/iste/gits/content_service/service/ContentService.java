package de.unistuttgart.iste.gits.content_service.service;

import de.unistuttgart.iste.gits.common.event.ChapterChange;
import de.unistuttgart.iste.gits.common.event.CrudOperation;
import de.unistuttgart.iste.gits.common.event.ResourceUpdate;
import de.unistuttgart.iste.gits.common.util.PaginationUtil;
import de.unistuttgart.iste.gits.content_service.dapr.TopicPublisher;
import de.unistuttgart.iste.gits.content_service.persistence.dao.ContentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.dao.TagEntity;
import de.unistuttgart.iste.gits.content_service.persistence.mapper.ContentMapper;
import de.unistuttgart.iste.gits.content_service.persistence.repository.ContentRepository;
import de.unistuttgart.iste.gits.content_service.persistence.repository.TagRepository;
import de.unistuttgart.iste.gits.content_service.validation.ContentValidator;
import de.unistuttgart.iste.gits.generated.dto.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ContentService {

    private final ContentRepository contentRepository;
    private final TagRepository tagRepository;
    private final ContentMapper contentMapper;
    private final ContentValidator contentValidator;
    final TagSynchronizer tagSynchronization;
    private final TopicPublisher topicPublisher;

    public ContentPayload getAllContents() {
        return createContentPayload(contentRepository.findAll()
                .stream()
                .map(contentMapper::entityToDto)
                .toList());
    }

    /**
     * Deletes Content by ID
     *
     * @param uuid ID of Content
     * @return ID of removed Content Entity
     */
    public UUID deleteContent(UUID uuid) {
        requireContentExisting(uuid);

        ContentEntity deletedEntity = contentRepository.getReferenceById(uuid);

        contentRepository.delete(deletedEntity);

        //publish changes
        topicPublisher.notifyChange(deletedEntity, CrudOperation.DELETE);
        topicPublisher.informContentDependantServices(List.of(deletedEntity.getId()), CrudOperation.DELETE);

        return uuid;
    }

    /**
     * Checks if a Content with the given id exists. If not, an EntityNotFoundException is thrown.
     *
     * @param id The id of the Content to check.
     * @throws EntityNotFoundException If a Content with the given id does not exist.
     */
    public void requireContentExisting(UUID id) {
        if (!contentRepository.existsById(id)) {
            throw new EntityNotFoundException("Content with id " + id + " not found");
        }
    }

    public ContentPayload getContentsById(List<UUID> ids) {
        return createContentPayload(contentRepository.findByIdIn(ids)
                .stream()
                .map(contentMapper::entityToDto)
                .toList());
    }

    public List<List<Content>> getContentsByChapterIds(List<UUID> chapterIds) {
        List<List<Content>> result = new ArrayList<>(chapterIds.size());

        // get a list containing all contents with a matching chapter id, then map them by chapter id (multiple
        // contents might have the same chapter id)
        Map<UUID, List<Content>> contentsByChapterId = contentRepository.findByChapterIdIn(chapterIds).stream()
                .map(contentMapper::entityToDto)
                .collect(Collectors.groupingBy(content -> content.getMetadata().getChapterId()));

        // put the different groups of chapters into the result list such that the order matches the order
        // of chapter ids given by the chapterIds argument
        for (UUID chapterId : chapterIds) {
            List<Content> contents = contentsByChapterId.getOrDefault(chapterId, Collections.emptyList());

            result.add(contents);
        }

        return result;
    }

    private ContentPayload createContentPayload(List<Content> contents) {
        // this is temporary until we have a proper pagination implementation
        return new ContentPayload(contents, PaginationUtil.unpagedPaginationInfo(contents.size()));
    }

    /**
     * creates Link between Content Entity and Tag
     *
     * @param id      content ID
     * @param tagName name of Tag
     * @return DTO with updated Content Entity
     */
    public Content addTagToContent(UUID id, String tagName) {
        requireContentExisting(id);
        ContentEntity content = contentRepository.getReferenceById(id);
        // The repository should return at most one tag as one content should not have multiple tags with the same name
        List<TagEntity> currentTagsWithThisName = tagRepository.findByContentIdAndTagName(content.getId(), tagName);
        if (currentTagsWithThisName.isEmpty()) {
            List<TagEntity> existingTags = tagRepository.findByName(tagName);
            TagEntity tagEntity;
            if (existingTags.isEmpty()) {
                // There is no tag with this name
                tagEntity = TagEntity.fromName(tagName);
                tagRepository.save(tagEntity);
            } else {
                tagEntity = existingTags.get(0);
            }
            content.addToTags(tagEntity);
            tagEntity.addToContents(content);
        }
        return contentMapper.entityToDto(content);
    }

    /**
     * removes Link between Content Entity and Tag in both directions
     *
     * @param id      of the Content Entity
     * @param tagName name of the Tag
     * @return DTO with updated Content Entity
     */
    public Content removeTagFromContent(UUID id, String tagName) {
        requireContentExisting(id);
        ContentEntity content = contentRepository.getReferenceById(id);
        // The repository should return at most one tag as one content should not have multiple tags with the same name
        List<TagEntity> currentTagsWithThisName = tagRepository.findByContentIdAndTagName(content.getId(), tagName);
        for (TagEntity tagEntity : currentTagsWithThisName) {
            content.removeFromTags(tagEntity);
            tagEntity.removeFromContents(content);
        }
        return contentMapper.entityToDto(content);
    }

    /**
     * Creates a Media Entity with given input
     *
     * @param input to be used as basis of creation
     * @return DTO with created Assessment Entity
     */
    public MediaContent createMediaContent(CreateMediaContentInput input) {
        contentValidator.validateCreateMediaContentInput(input);
        ContentEntity contentEntity = contentMapper.mediaContentDtoToEntity(input);
        return contentMapper.mediaContentEntityToDto(createContent(contentEntity, input.getMetadata().getTagNames()));
    }

    /**
     * Updates a Media Entity with given input
     *
     * @param input containing updated version of entity
     * @return DTO with updated entity
     */
    public MediaContent updateMediaContent(UpdateMediaContentInput input) {
        contentValidator.validateUpdateMediaContentInput(input);
        requireContentExisting(input.getId());

        ContentEntity oldContentEntity = contentRepository.getReferenceById(input.getId());
        ContentEntity updatedContentEntity = contentMapper.mediaContentDtoToEntity(input,
                oldContentEntity.getMetadata().getType());

        updatedContentEntity = updateContent(oldContentEntity, updatedContentEntity, input.getMetadata().getTagNames());
        return contentMapper.mediaContentEntityToDto(updatedContentEntity);
    }

    /**
     * Creates an Assessment Entity with given input
     *
     * @param input to be used as basis of creation
     * @return DTO with created Assessment Entity
     */
    public Assessment createAssessment(CreateAssessmentInput input) {
        contentValidator.validateCreateAssessmentContentInput(input);

        ContentEntity contentEntity = createContent(contentMapper.assessmentDtoToEntity(input),
                input.getMetadata().getTagNames());
        return contentMapper.assessmentEntityToDto(contentEntity);
    }

    /**
     * Updates an Assessment Entity with given input
     *
     * @param input containing updated version of entity
     * @return DTO with updated entity
     */
    public Assessment updateAssessment(UpdateAssessmentInput input) {
        contentValidator.validateUpdateAssessmentContentInput(input);
        requireContentExisting(input.getId());

        ContentEntity oldContentEntity = contentRepository.getReferenceById(input.getId());
        ContentEntity updatedContentEntity = contentMapper.assessmentDtoToEntity(input,
                oldContentEntity.getMetadata().getType());

        updatedContentEntity = updateContent(oldContentEntity, updatedContentEntity, input.getMetadata().getTagNames());
        return contentMapper.assessmentEntityToDto(updatedContentEntity);
    }

    /**
     * Generified Content Entity create method.
     *
     * @param contentEntity entity to be saved to database
     * @param tags          associated to the entity
     * @param <T>           all Entities that inherit from content Entity
     * @return entity saved
     */
    private <T extends ContentEntity> T createContent(T contentEntity, List<String> tags) {
        checkPermissionsForChapter(contentEntity.getMetadata().getChapterId());

        tagSynchronization.synchronizeTags(contentEntity, tags);
        contentEntity = contentRepository.save(contentEntity);

        topicPublisher.notifyChange(contentEntity, CrudOperation.CREATE);

        return contentEntity;
    }

    /**
     * Generified Content Entity update method.
     *
     * @param oldContentEntity     entity to be replaced
     * @param updatedContentEntity updated version of above entity
     * @param tags                 associated to the entity
     * @param <T>                  all Entities that inherit from content Entity
     * @return entity saved
     */
    private <T extends ContentEntity> T updateContent(T oldContentEntity, T updatedContentEntity, List<String> tags) {
        if (!oldContentEntity.getMetadata().getChapterId().equals(updatedContentEntity.getMetadata().getChapterId())) {
            checkPermissionsForChapter(updatedContentEntity.getMetadata().getChapterId());
        }

        tagSynchronization.synchronizeTags(updatedContentEntity, tags);
        updatedContentEntity = contentRepository.save(updatedContentEntity);

        //TODO: publish update if chapter ID is changed and added to a different course as a result
        if (!oldContentEntity.getMetadata().getChapterId().equals(updatedContentEntity.getMetadata().getChapterId())) {
            topicPublisher.informContentDependantServices(List.of(updatedContentEntity.getId()), CrudOperation.UPDATE);
        }

        return updatedContentEntity;
    }

    /**
     * method to forward received resource updates with additional information to course association topic
     *
     * @param dto resource update dto
     */
    public void forwardResourceUpdates(ResourceUpdate dto) {

        // completeness check of input
        if (dto.getEntityId() == null || dto.getContentIds() == null || dto.getContentIds().isEmpty() || dto.getOperation() == null) {
            throw new NullPointerException("incomplete message received: all fields of a message must be non-null");
        }

        // find all chapter IDs
        List<UUID> contentEntities = contentRepository.findAllById(dto.getContentIds())
                .stream()
                .map(contentEntity -> contentEntity.getMetadata()
                        .getChapterId())
                .toList();


        topicPublisher.forwardChange(dto.getEntityId(), contentEntities, dto.getOperation());
    }

    /**
     * Method that cascades the deletion of chapters to chapter-dependant-content
     *
     * @param dto
     */
    public void cascadeContentDeletion(ChapterChange dto) {
        List<UUID> chapterIds;
        List<UUID> contentIds = new ArrayList<>();

        // ignore any messages that are not deletion messages
        if (!dto.getOperation().equals(CrudOperation.DELETE)) {
            return;
        }

        chapterIds = dto.getChapterIds();

        // make sure message is complete
        if (chapterIds == null || chapterIds.isEmpty()) {
            throw new NullPointerException("incomplete message received: all fields of a message must be non-null");
        }

        List<ContentEntity> contentEntities = contentRepository.findByChapterIdIn(chapterIds);

        for (ContentEntity entity : contentEntities) {
            contentIds.add(entity.getId());
            contentRepository.delete(entity);
        }

        // inform dependant services that content entities were deleted
        topicPublisher.informContentDependantServices(contentIds, CrudOperation.DELETE);

    }


    @SuppressWarnings("java:S1172")
    private void checkPermissionsForChapter(UUID chapterId) {
        // not implemented yet
    }
}
