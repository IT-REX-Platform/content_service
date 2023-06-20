package de.unistuttgart.iste.gits.content_service.service;

import de.unistuttgart.iste.gits.common.util.PaginationUtil;
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

    public ContentPayload getAllContents() {
        return createContentPayload(contentRepository.findAll()
                .stream()
                .map(contentMapper::entityToDto)
                .toList());
    }

    public UUID deleteContent(UUID uuid) {
        requireContentExisting(uuid);
        contentRepository.deleteById(uuid);
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

    public MediaContent createMediaContent(CreateMediaContentInput input) {
        contentValidator.validateCreateMediaContentInput(input);
        ContentEntity contentEntity = contentMapper.mediaContentDtoToEntity(input);
        return contentMapper.mediaContentEntityToDto(createContent(contentEntity, input.getMetadata().getTagNames()));
    }

    public MediaContent updateMediaContent(UpdateMediaContentInput input) {
        contentValidator.validateUpdateMediaContentInput(input);
        requireContentExisting(input.getId());

        ContentEntity oldContentEntity = contentRepository.getReferenceById(input.getId());
        ContentEntity updatedContentEntity = contentMapper.mediaContentDtoToEntity(input,
                oldContentEntity.getMetadata().getType());

        updatedContentEntity = updateContent(oldContentEntity, updatedContentEntity, input.getMetadata().getTagNames());
        return contentMapper.mediaContentEntityToDto(updatedContentEntity);
    }

    public Assessment createAssessment(CreateAssessmentInput input) {
        contentValidator.validateCreateAssessmentContentInput(input);

        ContentEntity contentEntity = createContent(contentMapper.assessmentDtoToEntity(input),
                input.getMetadata().getTagNames());
        return contentMapper.assessmentEntityToDto(contentEntity);
    }

    public Assessment updateAssessment(UpdateAssessmentInput input) {
        contentValidator.validateUpdateAssessmentContentInput(input);
        requireContentExisting(input.getId());

        ContentEntity oldContentEntity = contentRepository.getReferenceById(input.getId());
        ContentEntity updatedContentEntity = contentMapper.assessmentDtoToEntity(input,
                oldContentEntity.getMetadata().getType());

        updatedContentEntity = updateContent(oldContentEntity, updatedContentEntity, input.getMetadata().getTagNames());
        return contentMapper.assessmentEntityToDto(updatedContentEntity);
    }

    private <T extends ContentEntity> T createContent(T contentEntity, List<String> tags) {
        checkPermissionsForChapter(contentEntity.getMetadata().getChapterId());

        tagSynchronization.synchronizeTags(contentEntity, tags);
        contentEntity = contentRepository.save(contentEntity);

        return contentEntity;
    }

    private <T extends ContentEntity> T updateContent(T oldContentEntity, T updatedContentEntity, List<String> tags) {
        if (!oldContentEntity.getMetadata().getChapterId().equals(updatedContentEntity.getMetadata().getChapterId())) {
            checkPermissionsForChapter(updatedContentEntity.getMetadata().getChapterId());
        }

        tagSynchronization.synchronizeTags(updatedContentEntity, tags);
        updatedContentEntity = contentRepository.save(updatedContentEntity);
        return updatedContentEntity;
    }

    @SuppressWarnings("java:S1172")
    private void checkPermissionsForChapter(UUID chapterId) {
        // not implemented yet
    }
}
