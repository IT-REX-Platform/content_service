package de.unistuttgart.iste.gits.content_service.service;


import de.unistuttgart.iste.gits.common.event.*;
import de.unistuttgart.iste.gits.common.exception.IncompleteEventMessageException;
import de.unistuttgart.iste.gits.common.util.PaginationUtil;
import de.unistuttgart.iste.gits.content_service.dapr.TopicPublisher;
import de.unistuttgart.iste.gits.content_service.persistence.entity.*;
import de.unistuttgart.iste.gits.content_service.persistence.mapper.ContentMapper;
import de.unistuttgart.iste.gits.content_service.persistence.repository.*;
import de.unistuttgart.iste.gits.content_service.validation.ContentValidator;
import de.unistuttgart.iste.gits.generated.dto.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.unistuttgart.iste.gits.common.util.GitsCollectionUtils.groupIntoSubLists;

@Service
@RequiredArgsConstructor
@Transactional
public class ContentService {

    private final ContentRepository contentRepository;
    private final SectionRepository sectionRepository;
    private final UserProgressDataRepository userProgressDataRepository;
    private final StageService stageService;
    private final ContentMapper contentMapper;
    private final ContentValidator contentValidator;
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
        ContentEntity deletedEntity = requireContentExisting(uuid);

        UUID removedId = deleteContentAndRemoveDependencies(deletedEntity);

        topicPublisher.informContentDependentServices(List.of(removedId), CrudOperation.DELETE);
        return uuid;
    }

    /**
     * Checks if a Content with the given id exists. If not, an EntityNotFoundException is thrown.
     *
     * @param id The id of the Content to check.
     * @return The Content with the given id.
     * @throws EntityNotFoundException If a Content with the given id does not exist.
     */
    public ContentEntity requireContentExisting(UUID id) {
        return contentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Content with id " + id + " not found"));
    }

    /**
     * Like {@link #findContentsById(List)} but throws an EntityNotFoundException if a content with a given id
     * does not exist.
     *
     * @param ids the ids of the contents to find
     * @return the contents with the given ids. The size of the list will be the same as the size of the given list.
     * The order of the contents will match the order of the given ids.
     * @throws EntityNotFoundException If a content with a given id does not exist.
     */
    public List<Content> getContentsById(List<UUID> ids) {
        List<Content> contents = findContentsById(ids);

        List<UUID> notFound = new ArrayList<>();
        for (int i = 0; i < contents.size(); i++) {
            if (contents.get(i) == null) {
                notFound.add(ids.get(i));
            }
        }

        if (!notFound.isEmpty()) {
            throw new EntityNotFoundException("Contents with ids "
                                              + notFound.stream().map(UUID::toString).collect(Collectors.joining(", "))
                                              + " not found");
        }

        return contents;
    }

    /**
     * Finds contents by their ids. If a content with a given id does not exist,
     * the corresponding list entry will be null.
     * This way, the returned list will always have the same size as the given list
     * and the order of the contents will match the order of the given ids.
     *
     * @param ids The ids of the contents to find.
     * @return a list of nullable contents. The size of the list will be the same as the size of the given list.
     * The order of the contents will match the order of the given ids.
     */
    public List<Content> findContentsById(List<UUID> ids) {
        return ids.stream()
                .map(contentRepository::findById)
                // map to content dto or null if content does not exist
                .map(optionalContent -> optionalContent.map(contentMapper::entityToDto).orElse(null))
                .toList();
    }

    /**
     * Returns a list of contents for each given course id. The order of the lists will match the order of the given
     * course ids.
     *
     * @param courseIds the ids of the courses to get the contents for
     * @return a list of lists of contents. The order of the lists will match the order of the given course ids.
     */
    public List<List<Content>> getContentsByCourseIds(List<UUID> courseIds) {
        List<ContentEntity> matchingContents = contentRepository.findByCourseIdIn(courseIds);
        List<Content> contentDtos = matchingContents.stream().map(contentMapper::entityToDto).toList();

        return groupIntoSubLists(contentDtos, courseIds, content -> content.getMetadata().getCourseId());
    }

    /**
     * Returns a list of lists of contents for each given chapter id. The order of the lists will match the order of the
     * given chapter ids.
     *
     * @param chapterIds the ids of the chapters to get the contents for
     * @return a list of lists of contents. The order of the lists will match the order of the given chapter ids.
     */
    public List<List<Content>> getContentsByChapterIds(List<UUID> chapterIds) {
        List<Content> allMatchingContents = contentRepository.findByChapterIdIn(chapterIds)
                .stream()
                .map(contentMapper::entityToDto)
                .toList();

        return groupIntoSubLists(allMatchingContents, chapterIds, content -> content.getMetadata().getChapterId());
    }

    private ContentPayload createContentPayload(List<Content> contents) {
        // this is until we have a proper pagination implementation
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
        ContentEntity content = requireContentExisting(id);

        Set<String> newTags = new HashSet<>(content.getMetadata().getTags());
        newTags.add(tagName);
        content.getMetadata().setTags(newTags);
        content = contentRepository.save(content);
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
        ContentEntity content = requireContentExisting(id);

        Set<String> newTags = new HashSet<>(content.getMetadata().getTags());
        newTags.remove(tagName);
        content.getMetadata().setTags(newTags);
        content = contentRepository.save(content);

        return contentMapper.entityToDto(content);
    }

    /**
     * Creates a Media Entity with given input
     *
     * @param input    to be used as basis of creation
     * @param courseId ID of the course the content belongs to
     * @return DTO with created Assessment Entity
     */
    public MediaContent createMediaContent(CreateMediaContentInput input, UUID courseId) {
        contentValidator.validateCreateMediaContentInput(input);
        ContentEntity contentEntity = contentMapper.mediaContentDtoToEntity(input);
        return contentMapper.mediaContentEntityToDto(createContent(contentEntity,
                input.getMetadata().getTagNames(), courseId));
    }

    /**
     * Updates a Media Entity with given input
     *
     * @param input containing updated version of entity
     * @return DTO with updated entity
     */
    public MediaContent updateMediaContent(UUID contentId, UpdateMediaContentInput input) {
        contentValidator.validateUpdateMediaContentInput(input);

        ContentEntity oldContentEntity = requireContentExisting(contentId);
        ContentEntity updatedContentEntity = contentMapper.mediaContentDtoToEntity(contentId, input,
                oldContentEntity.getMetadata().getType());

        updatedContentEntity = updateContent(oldContentEntity, updatedContentEntity);
        return contentMapper.mediaContentEntityToDto(updatedContentEntity);
    }

    /**
     * Creates an Assessment Entity with given input
     *
     * @param input    to be used as basis of creation
     * @param courseId ID of the course the content belongs to
     * @return DTO with created Assessment Entity
     */
    public Assessment createAssessment(CreateAssessmentInput input, UUID courseId) {
        contentValidator.validateCreateAssessmentContentInput(input);

        ContentEntity contentEntity = createContent(contentMapper.assessmentDtoToEntity(input),
                input.getMetadata().getTagNames(), courseId);
        return contentMapper.assessmentEntityToDto(contentEntity);
    }

    /**
     * Updates an Assessment Entity with given input
     *
     * @param input containing updated version of entity
     * @return DTO with updated entity
     */
    public Assessment updateAssessment(UUID contentId, UpdateAssessmentInput input) {
        contentValidator.validateUpdateAssessmentContentInput(input);

        ContentEntity oldContentEntity = requireContentExisting(contentId);
        ContentEntity updatedContentEntity = contentMapper.assessmentDtoToEntity(contentId, input,
                oldContentEntity.getMetadata().getType());

        updatedContentEntity = updateContent(oldContentEntity, updatedContentEntity);
        return contentMapper.assessmentEntityToDto(updatedContentEntity);
    }

    /**
     * Generified Content Entity create method.
     *
     * @param contentEntity entity to be saved to database
     * @param <T>           all Entities that inherit from content Entity
     * @return entity saved
     */
    private <T extends ContentEntity> T createContent(T contentEntity, List<String> tags, UUID courseId) {
        contentEntity.getMetadata().setCourseId(courseId);
        contentEntity = contentRepository.save(contentEntity);

        topicPublisher.notifyChange(contentEntity, CrudOperation.CREATE);

        return contentEntity;
    }

    /**
     * Generified Content Entity update method.
     *
     * @param oldContentEntity     entity to be replaced
     * @param updatedContentEntity updated version of above entity
     * @param <T>                  all Entities that inherit from content Entity
     * @return entity saved
     */
    private <T extends ContentEntity> T updateContent(T oldContentEntity, T updatedContentEntity) {
        updatedContentEntity.getMetadata().setCourseId(oldContentEntity.getMetadata().getCourseId());
        updatedContentEntity = contentRepository.save(updatedContentEntity);

        // if the content is assigned to a different chapter course Links need to be potentially updated and therefore
        // an Update request is sent to the resource services
        if (!oldContentEntity.getMetadata().getChapterId().equals(updatedContentEntity.getMetadata().getChapterId())) {
            topicPublisher.informContentDependentServices(List.of(updatedContentEntity.getId()), CrudOperation.UPDATE);
        }

        return updatedContentEntity;
    }

    /**
     * method to forward received resource updates with additional information to course association topic
     *
     * @param dto resource update dto
     */
    public void forwardResourceUpdates(ResourceUpdateEvent dto) throws IncompleteEventMessageException {

        // completeness check of input
        if (dto.getEntityId() == null || dto.getContentIds() == null || dto.getOperation() == null) {
            throw new IncompleteEventMessageException(IncompleteEventMessageException.ERROR_INCOMPLETE_MESSAGE);
        }

        // find all chapter IDs
        List<UUID> contentEntityIds = contentRepository.findAllById(dto.getContentIds())
                .stream()
                .map(contentEntity -> contentEntity.getMetadata().getChapterId())
                .toList();


        topicPublisher.forwardChange(dto.getEntityId(), contentEntityIds, dto.getOperation());
    }

    /**
     * Method that cascades the deletion of chapters to chapter-dependant-content
     *
     * @param dto message containing information about to be deleted entities
     */
    public void cascadeContentDeletion(ChapterChangeEvent dto) throws IncompleteEventMessageException {
        List<UUID> contentIds = new ArrayList<>();
        List<UUID> chapterIds = dto.getChapterIds();

        // make sure message is complete
        if (chapterIds == null || chapterIds.isEmpty() || dto.getOperation() == null) {
            throw new IncompleteEventMessageException(IncompleteEventMessageException.ERROR_INCOMPLETE_MESSAGE);
        }

        // ignore any messages that are not deletion messages
        if (!dto.getOperation().equals(CrudOperation.DELETE)) {
            return;
        }

        List<ContentEntity> contentEntities = contentRepository.findByChapterIdIn(chapterIds);

        for (ContentEntity entity : contentEntities) {
            // remove all links from stages to content
            // and collect IDs of deleted content entities
            contentIds.add(deleteContentAndRemoveDependencies(entity));
        }

        if (!contentIds.isEmpty()) {
            // inform dependant services that content entities were deleted
            topicPublisher.informContentDependentServices(contentIds, CrudOperation.DELETE);
        }
    }

    /**
     * Removes a stage links to the content entity and then deleted the content entity afterward.
     * This also deletes the user progress data and unused tags
     * and publishes the changes applied to the content entity.
     *
     * @param contentEntity content entity to be deleted
     * @return the ID of the deleted content entity
     */
    private UUID deleteContentAndRemoveDependencies(ContentEntity contentEntity) {
        userProgressDataRepository.deleteByContentId(contentEntity.getId());
        // remove content from sections
        stageService.deleteContentLinksFromStages(contentEntity);

        contentRepository.delete(contentEntity);

        // publish changes applied to content entity
        topicPublisher.notifyChange(contentEntity, CrudOperation.DELETE);

        return contentEntity.getId();
    }

    public Map<UUID, List<Content>> getContentEntitiesSortedByChapterId(final List<UUID> chapterIds) {
        return contentRepository.findByChapterIdIn(chapterIds).stream()
                .map(contentMapper::entityToDto)
                .collect(Collectors.groupingBy(content -> content.getMetadata().getChapterId()));
    }

    /**
     * Returns a list of all skill types that are achievable by the user in the given chapters.
     * A skill type is achievable if there exists at least one assessment in a chapter that has this skill type.
     *
     * @param chapterIds the ids of the chapters to check
     * @return a list of all skill types that are achievable by the user in the given chapters.
     * The order of the list will match the order of the given chapter ids.
     */
    public List<List<SkillType>> getAchievableSkillTypesByChapterIds(List<UUID> chapterIds) {
        return chapterIds.stream()
                .map(this::getSkillTypesOfChapter)
                .toList();
    }

    @NotNull
    private List<SkillType> getSkillTypesOfChapter(UUID chapterId) {
        return contentRepository.findSkillTypesByChapterId(chapterId)
                .stream()
                // each content has a list of skill types, so we have to flatten the list of lists of skill types
                .flatMap(List::stream)
                // remove duplicates
                .distinct()
                .toList();
    }


    /**
     * returns an ordered list of content with no links to any section for a list of chapter ID.
     *
     * @param chapterIds list of chapter IDs in which content is to be searched for
     * @return ordered list of content that matches the order of the chapterID argument
     */
    public List<List<Content>> getContentWithNoSection(final List<UUID> chapterIds) {

        // get a list containing all sections for the given chapters, but not divided by chapter yet
        final List<SectionEntity> sectionEntities = sectionRepository.findByChapterIdInOrderByPosition(chapterIds);
        final List<ContentEntity> contentEntities = contentRepository.findByChapterIdIn(chapterIds);

        // collect optional and required content in form of a stream for further processing
        final List<StageEntity> availableStages = sectionEntities.stream()
                .map(SectionEntity::getStages)
                .flatMap(Collection::stream).toList();

        final Stream<Set<ContentEntity>> requiredContentStream = availableStages.stream()
                .map(StageEntity::getRequiredContents);

        final Stream<Set<ContentEntity>> optionalContentStream = availableStages.stream()
                .map(StageEntity::getOptionalContents);

        // collect content from both required and optional sets in stages
        final Set<ContentEntity> contentEntitiesInSections = Stream.concat(requiredContentStream, optionalContentStream)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        final List<Content> contentsWithNoSection = contentEntities.stream()
                .filter(Predicate.not(contentEntitiesInSections::contains))
                .map(contentMapper::entityToDto)
                .toList();

        return groupIntoSubLists(contentsWithNoSection, chapterIds, content -> content.getMetadata().getChapterId());
    }

}
