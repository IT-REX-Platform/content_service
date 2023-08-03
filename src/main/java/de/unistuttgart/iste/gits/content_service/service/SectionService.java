package de.unistuttgart.iste.gits.content_service.service;

import de.unistuttgart.iste.gits.content_service.persistence.dao.StageEntity;
import de.unistuttgart.iste.gits.content_service.persistence.dao.SectionEntity;
import de.unistuttgart.iste.gits.content_service.persistence.mapper.SectionMapper;
import de.unistuttgart.iste.gits.content_service.persistence.repository.SectionRepository;
import de.unistuttgart.iste.gits.generated.dto.CreateSectionInput;
import de.unistuttgart.iste.gits.generated.dto.Section;
import de.unistuttgart.iste.gits.generated.dto.StageOrderInput;
import de.unistuttgart.iste.gits.generated.dto.UpdateSectionInput;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
     * @param input input object containing id and new name
     * @return updated Section object
     */
    public Section updateSection(UpdateSectionInput input) {

        requireSectionExisting(input.getId());
        //updates name only!
        SectionEntity sectionEntity = sectionRepository.getReferenceById(input.getId());
        sectionEntity.setName(input.getName());
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
     * changes the order of Stages within a Section
     *
     * @param input order list of stage IDs describing new Stage Order
     * @return updated Section with new Stage Order
     */
    public Section reorderStages(StageOrderInput input) {

        SectionEntity sectionEntity = sectionRepository.getReferenceById(input.getSectionId());

        //ensure received list is complete
        validateStageIds(input.getStageIds(), sectionEntity.getStages());

        for (StageEntity stageEntity : sectionEntity.getStages()) {

            int newPos = input.getStageIds().indexOf(stageEntity.getId());

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
     * find all Section for a Chapter ID
     *
     * @param uuid chapter ID
     * @return all Sections that have received chapter ID in form of a List
     */
    public List<Section> getSectionByChapterId(UUID uuid) {
        List<SectionEntity> entities = sectionRepository.findSectionEntitiesByChapterId(uuid);

        return entities.stream()
                .map(sectionMapper::entityToDto)
                .toList();
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
