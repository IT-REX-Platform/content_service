package de.unistuttgart.iste.gits.content_service.service;

import de.unistuttgart.iste.gits.content_service.persistence.dao.*;
import de.unistuttgart.iste.gits.content_service.persistence.mapper.StageMapper;
import de.unistuttgart.iste.gits.content_service.persistence.repository.*;
import de.unistuttgart.iste.gits.generated.dto.Stage;
import de.unistuttgart.iste.gits.generated.dto.UpdateStageInput;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class StageService {

    private final StageRepository stageRepository;
    private final SectionRepository sectionRepository;
    private final ContentRepository contentRepository;
    private final StageMapper stageMapper;

    /**
     * creates a new Stage for an existing Section
     *
     * @param sectionId Section ID the Stage belongs to
     * @return created Stage
     */
    public Stage createNewStage(UUID sectionId) {

        requireSectionExisting(sectionId);

        SectionEntity sectionEntity = sectionRepository.getReferenceById(sectionId);
        StageEntity stageEntity = StageEntity.builder()
                .sectionId(sectionId)
                .position(sectionEntity.getStages().size())
                .requiredContents(new HashSet<>())
                .optionalContent(new HashSet<>())
                .build();

        return stageMapper.entityToDto(stageRepository.save(stageEntity));
    }

    /**
     * Updates the Content of an existing Stage
     *
     * @param input Update Input. Fields must not be null
     * @return updated Stage
     */
    public Stage updateStage(UpdateStageInput input) {

        requireStageExisting(input.getId());

        // fetch old Stage Object
        StageEntity stageEntity = stageRepository.getReferenceById(input.getId());

        requireSectionExisting(stageEntity.getSectionId());

        // fetch Work Path, Stage belongs to
        SectionEntity sectionEntity = sectionRepository.getReferenceById(stageEntity.getSectionId());

        // set updated Content
        stageEntity.setRequiredContents(
                validateStageContent(
                        sectionEntity.getChapterId(),
                        input.getRequiredContents()
                ));

        stageEntity.setOptionalContent(
                validateStageContent(
                        sectionEntity.getChapterId(),
                        input.getOptionalContents()
                ));

        return stageMapper.entityToDto(stageRepository.save(stageEntity));
    }

    /**
     * validates that received content is located in the same chapter as the Section / Stage.
     * If Content is not part of the same chapter, the content is removed
     *
     * @param chapterId  chapter ID of the Section / Stage
     * @param contentIds List of Content IDs to be validated
     * @return Set of validated Content Entities
     */
    private Set<ContentEntity> validateStageContent(UUID chapterId, List<UUID> contentIds) {

        Set<ContentEntity> resultSet = new HashSet<>();

        List<ContentEntity> contentEntities = contentRepository.findContentEntitiesByIdIn(contentIds);

        for (ContentEntity contentEntity : contentEntities) {
            // only add content that is located in the same chapter as the Work-Path / Stage
            if (contentEntity.getMetadata().getChapterId().equals(chapterId)) {
                resultSet.add(contentEntity);
            }
        }

        return resultSet;
    }

    /**
     * Deletes a Stage via ID
     *
     * @param stageId Stage ID to be removed
     * @return ID of deleted Stage
     */
    public UUID deleteStage(UUID stageId) {

        requireStageExisting(stageId);

        StageEntity deletedStageEntity = stageRepository.getReferenceById(stageId);

        SectionEntity sectionEntity = sectionRepository.getReferenceById(deletedStageEntity.getSectionId());

        //if a stage is deleted all subsequent stages have to have their position moved up by 1 in the list
        for (StageEntity entity : sectionEntity.getStages()) {
            if (entity.getPosition() > deletedStageEntity.getPosition()) {
                //move entity one position up
                entity.setPosition(entity.getPosition() - 1);
                stageRepository.save(entity);
            }
        }
        // perform deletion
        stageRepository.delete(deletedStageEntity);

        return deletedStageEntity.getId();
    }


    /**
     * Checks if a Stage exists.
     *
     * @param uuid The id of the Stage to check.
     * @throws EntityNotFoundException If the chapter does not exist.
     */
    private void requireStageExisting(UUID uuid) {
        if (!stageRepository.existsById(uuid)) {
            throw new EntityNotFoundException("Stage with id " + uuid + " not found");
        }
    }

    /**
     * Checks if a Section exists.
     *
     * @param uuid The id of the Section to check.
     * @throws EntityNotFoundException If the chapter does not exist.
     */
    private void requireSectionExisting(UUID uuid) {
        if (uuid == null) {
            throw new NullPointerException("Section must be not null!");
        }
        if (!sectionRepository.existsById(uuid)) {
            throw new EntityNotFoundException("Section with id " + uuid + " not found");
        }
    }


}
