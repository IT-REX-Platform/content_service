package de.unistuttgart.iste.gits.content_service.service;

import de.unistuttgart.iste.gits.common.event.ChapterChangeEvent;
import de.unistuttgart.iste.gits.content_service.persistence.dao.SectionEntity;
import de.unistuttgart.iste.gits.content_service.persistence.dao.StageEntity;
import de.unistuttgart.iste.gits.content_service.persistence.mapper.SectionMapper;
import de.unistuttgart.iste.gits.content_service.persistence.repository.SectionRepository;
import de.unistuttgart.iste.gits.generated.dto.CreateSectionInput;
import de.unistuttgart.iste.gits.generated.dto.Section;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SectionService {

    private final SectionMapper sectionMapper;
    private final SectionRepository sectionRepository;

    /**
     * creates a new Section for a given chapterId and name
     *
     * @param input input object containing a chapter ID and name
     * @return new Section Object
     */
    public Section createSection(CreateSectionInput input) {
        SectionEntity sectionEntity = sectionRepository.save(
                SectionEntity.builder()
                        .name(input.getName())
                        .chapterId(input.getChapterId())
                        .stages(new HashSet<>())
                        .build()
        );
        return sectionMapper.entityToDto(sectionEntity);
    }

    /**
     * Updates the name of a Section
     *
     * @param sectionId ID of the section to be changed
     * @param name      new name for the section
     * @return updated Section object
     */
    public Section updateSectionName(UUID sectionId, String name) {

        requireSectionExisting(sectionId);
        //updates name only!
        SectionEntity sectionEntity = sectionRepository.getReferenceById(sectionId);
        sectionEntity.setName(name);
        sectionEntity = sectionRepository.save(sectionEntity);
        return sectionMapper.entityToDto(sectionEntity);
    }

    /**
     * deletes a Section via ID
     *
     * @param sectionId ID of Section
     * @return ID of deleted Object
     */
    public UUID deleteWorkPath(UUID sectionId) {
        requireSectionExisting(sectionId);

        sectionRepository.deleteById(sectionId);

        return sectionId;
    }

    /**
     * Deletes a Section and all its associated Stages.
     *
     * @param dto of Section to delete
     */
    public void cascadeSectionDeletion(ChapterChangeEvent dto) {
        List<UUID> chapterIds;
        List<SectionEntity> sections;

        chapterIds = dto.getChapterIds();

        // make sure message is complete
        if (chapterIds == null || chapterIds.isEmpty() || dto.getOperation() == null) {
            throw new NullPointerException("incomplete message received: all fields of a message must be non-null");
        }
        sections = sectionRepository.findByChapterIdIn(chapterIds);
        sectionRepository.deleteAllInBatch(sections);
    }

    /**
     * changes the order of Stages within a Section
     *
     * @param input order list of stage IDs describing new Stage Order
     * @return updated Section with new Stage Order
     */
    public Section reorderStages(UUID sectionId, List<UUID> input) {

        SectionEntity sectionEntity = sectionRepository.getReferenceById(sectionId);

        //ensure received list is complete
        validateStageIds(input, sectionEntity.getStages());

        for (StageEntity stageEntity : sectionEntity.getStages()) {

            int newPos = input.indexOf(stageEntity.getId());

            stageEntity.setPosition(newPos);
        }

        // persist changes
        sectionRepository.save(sectionEntity);

        return sectionMapper.entityToDto(sectionEntity);
    }

    /**
     * ensures received ID list is complete
     *
     * @param receivedStageIds received ID list
     * @param stageEntities    found entities in database
     */
    private void validateStageIds(List<UUID> receivedStageIds, Set<StageEntity> stageEntities) {
        if (receivedStageIds.size() > stageEntities.size()) {
            throw new EntityNotFoundException("Stage ID list contains more elements than expected");
        }
        List<UUID> stageIds = stageEntities.stream().map(StageEntity::getId).toList();
        for (UUID stageId : stageIds) {
            if (!receivedStageIds.contains(stageId)) {
                throw new EntityNotFoundException("Incomplete Stage ID list received");
            }
        }
    }

    /**
     * Gets all sections for multiple chapters.
     * @param chapterIds The ids of the chapters to get the sections for.
     * @return A list of lists of sections. The outer list contains sublists which each contain the sections
     *         for one chapter.
     */
    public List<List<Section>> getSectionsByChapterIds(List<UUID> chapterIds) {
        List<List<Section>> result = new ArrayList<>(chapterIds.size());

        // get a list containing all sections for the given chapters, but not divided by chapter yet
        List<SectionEntity> entities = sectionRepository.findByChapterIdIn(chapterIds);

        // map the different sections into groups by chapter
        Map<UUID, List<Section>> sectionsByChapterId = entities.stream()
                .map(sectionMapper::entityToDto)
                .collect(Collectors.groupingBy(Section::getChapterId));

        // put the different groups of sections into the result list such that the order matches the order of chapter
        // ids given in the chapterIds argument
        for(UUID chapterId : chapterIds) {
            List<Section> sections = sectionsByChapterId.getOrDefault(chapterId, Collections.emptyList());
            result.add(sections);
        }

        return result;
    }

    /**
     * Checks if a Section exists.
     *
     * @param uuid The id of the Section to check.
     * @throws EntityNotFoundException If the chapter does not exist.
     */
    private void requireSectionExisting(UUID uuid) {
        if (!sectionRepository.existsById(uuid)) {
            throw new EntityNotFoundException("Section with id " + uuid + " not found");
        }
    }


}
