package de.unistuttgart.iste.gits.content_service.service;

import de.unistuttgart.iste.gits.content_service.persistence.dao.StageEntity;
import de.unistuttgart.iste.gits.content_service.persistence.dao.SectionEntity;
import de.unistuttgart.iste.gits.content_service.persistence.mapper.ContentMapper;
import de.unistuttgart.iste.gits.content_service.persistence.mapper.StageMapper;
import de.unistuttgart.iste.gits.content_service.persistence.mapper.SectionMapper;
import de.unistuttgart.iste.gits.content_service.persistence.repository.SectionRepository;
import de.unistuttgart.iste.gits.generated.dto.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SectionServiceTest {

    private final StageMapper stageMapper = new StageMapper(new ContentMapper(new ModelMapper()));
    private final SectionMapper sectionMapper = new SectionMapper(stageMapper);
    private final SectionRepository sectionRepository = Mockito.mock(SectionRepository.class);

    private final SectionService sectionService = new SectionService(sectionMapper, sectionRepository);

    @Test
    void createSectionTest() {
        //init
        CreateSectionInput input = CreateSectionInput.builder()
                .setChapterId(UUID.randomUUID())
                .setName("Test Section")
                .build();

        SectionEntity sectionEntity = SectionEntity.builder()
                .name(input.getName())
                .id(UUID.randomUUID())
                .chapterId(input.getChapterId())
                .stages(new HashSet<>()).build();

        Section expectedResult = Section.builder()
                .setId(sectionEntity.getId())
                .setName(sectionEntity.getName())
                .setChapterId(sectionEntity.getChapterId())
                .setStages(new ArrayList<>())
                .build();

        //mock database
        when(sectionRepository.save(any())).thenReturn(sectionEntity);

        // execute method under test
        Section result = sectionService.createSection(input);

        assertEquals(expectedResult, result);
        assertEquals(expectedResult.getId(), result.getId());
        assertEquals(expectedResult.getName(), result.getName());
        assertEquals(expectedResult.getChapterId(), result.getChapterId());
        assertEquals(expectedResult.getStages(), result.getStages());
    }

    @Test
    void updateSectionTest() {
        UUID sectionId = UUID.randomUUID();
        UpdateSectionInput input = UpdateSectionInput.builder()
                .setId(sectionId)
                .setName("Test Section")
                .build();

        SectionEntity oldSectionEntity = SectionEntity.builder()
                .name("This is a Section")
                .id(sectionId)
                .chapterId(UUID.randomUUID())
                .stages(new HashSet<>()).build();

        SectionEntity newSectionEntity = SectionEntity.builder()
                .name(input.getName())
                .id(sectionId)
                .chapterId(oldSectionEntity.getChapterId())
                .stages(new HashSet<>()).build();

        Section expectedResult = Section.builder()
                .setId(newSectionEntity.getId())
                .setName(newSectionEntity.getName())
                .setChapterId(newSectionEntity.getChapterId())
                .setStages(new ArrayList<>())
                .build();

        //mock database
        when(sectionRepository.existsById(input.getId())).thenReturn(true);
        when(sectionRepository.getReferenceById(input.getId())).thenReturn(oldSectionEntity);
        when(sectionRepository.save(any())).thenReturn(newSectionEntity);

        // execute method under test
        Section result = sectionService.updateSection(input);

        verify(sectionRepository, times(1)).save(newSectionEntity);

        assertEquals(expectedResult, result);
        assertEquals(expectedResult.getId(), result.getId());
        assertEquals(expectedResult.getName(), result.getName());
        assertEquals(expectedResult.getChapterId(), result.getChapterId());
        assertEquals(expectedResult.getStages(), result.getStages());
    }

    @Test
    void updateNoneExistingSectionTest() {
        UUID sectionId = UUID.randomUUID();
        UpdateSectionInput input = UpdateSectionInput.builder()
                .setId(sectionId)
                .setName("Test Section")
                .build();

        //mock database
        when(sectionRepository.existsById(input.getId())).thenReturn(false);

        // execute method under test
        assertThrows(EntityNotFoundException.class, () -> sectionService.updateSection(input));
    }

    // case: update Section with existing Stages
    @Test
    void updateSectionWithStagesTest() {
        UUID sectionId = UUID.randomUUID();
        UpdateSectionInput input = UpdateSectionInput.builder()
                .setId(sectionId)
                .setName("Test Section")
                .build();

        SectionEntity oldSectionEntity = SectionEntity.builder()
                .name("This is a Section")
                .id(sectionId)
                .chapterId(UUID.randomUUID())
                .stages(
                        Set.of(
                                buildStageEntity(sectionId, 0),
                                buildStageEntity(sectionId, 1)
                        )
                ).build();

        SectionEntity newSectionEntity = SectionEntity.builder()
                .name(input.getName())
                .id(sectionId)
                .chapterId(oldSectionEntity.getChapterId())
                .stages(oldSectionEntity.getStages())
                .build();


        Section expectedResult = Section.builder()
                .setId(newSectionEntity.getId())
                .setName(newSectionEntity.getName())
                .setChapterId(newSectionEntity.getChapterId())
                .setStages(newSectionEntity.getStages().stream().map(stageMapper::entityToDto).toList())
                .build();

        //mock database
        when(sectionRepository.existsById(input.getId())).thenReturn(true);
        when(sectionRepository.getReferenceById(input.getId())).thenReturn(oldSectionEntity);
        when(sectionRepository.save(any())).thenReturn(newSectionEntity);

        // execute method under test
        Section result = sectionService.updateSection(input);

        verify(sectionRepository, times(1)).save(newSectionEntity);

        assertEquals(expectedResult.getId(), result.getId());
        assertEquals(expectedResult.getName(), result.getName());
        assertEquals(expectedResult.getChapterId(), result.getChapterId());
        assertEquals(expectedResult.getStages(), result.getStages());
        assertEquals(expectedResult, result);
    }

    @Test
    void deleteSection() {
        UUID input = UUID.randomUUID();

        //mock database
        when(sectionRepository.existsById(input)).thenReturn(true);
        doNothing().when(sectionRepository).deleteById(input);

        UUID result = sectionService.deleteWorkPath(input);

        verify(sectionRepository, times(1)).deleteById(input);
        assertEquals(input, result);
    }

    @Test
    void deleteInvalidIdSection() {
        UUID input = UUID.randomUUID();

        //mock database
        when(sectionRepository.existsById(input)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> sectionService.deleteWorkPath(input));
    }

    // case: valid input provided
    @Test
    void reorderStagesTest() {

        UUID sectionId = UUID.randomUUID();

        Set<StageEntity> stageEntities = Set.of(
                buildStageEntity(sectionId, 0),
                buildStageEntity(sectionId, 1),
                buildStageEntity(sectionId, 2),
                buildStageEntity(sectionId, 3)
        );

        SectionEntity sectionEntity = SectionEntity.builder()
                .id(sectionId)
                .name("Section 1")
                .chapterId(UUID.randomUUID())
                .stages(stageEntities)
                .build();

        List<UUID> sortedStageIds = stageEntities.stream().map(StageEntity::getId).sorted().toList();


        StageOrderInput stageOrderInput = StageOrderInput.builder()
                .setSectionId(sectionId)
                .setStageIds(sortedStageIds)
                .build();

        //mock database
        when(sectionRepository.getReferenceById(stageOrderInput.getSectionId())).thenReturn(sectionEntity);
        when(sectionRepository.save(any())).thenReturn(sectionEntity);

        Section result = sectionService.reorderStages(stageOrderInput);

        verify(sectionRepository, times(1)).getReferenceById(sectionId);
        verify(sectionRepository, times(1)).save(any());

        for (Stage stage : result.getStages()) {
            assertEquals(sortedStageIds.indexOf(stage.getId()), stage.getPosition());
        }
    }

    // case: received stage ID list contains elements that are not part of the work Path
    @Test
    void reorderStagesInvalidStageListTest() {

        UUID sectionId = UUID.randomUUID();

        List<StageEntity> stageEntities = List.of(
                buildStageEntity(sectionId, 0),
                buildStageEntity(sectionId, 1),
                buildStageEntity(sectionId, 2),
                buildStageEntity(sectionId, 3)
        );

        SectionEntity sectionEntity = SectionEntity.builder()
                .id(sectionId)
                .name("Section 1")
                .chapterId(UUID.randomUUID())
                .stages(Set.copyOf(stageEntities.subList(0, 2)))
                .build();

        List<UUID> sortedStageIds = stageEntities.stream()
                .map(StageEntity::getId)
                .sorted()
                .toList();


        StageOrderInput stageOrderInput = StageOrderInput.builder()
                .setSectionId(sectionId)
                .setStageIds(sortedStageIds)
                .build();

        //mock database
        when(sectionRepository.getReferenceById(stageOrderInput.getSectionId())).thenReturn(sectionEntity);
        when(sectionRepository.save(any())).thenReturn(sectionEntity);

        assertThrows(EntityNotFoundException.class, () -> sectionService.reorderStages(stageOrderInput));

    }

    // case: received stage ID list is incomplete
    @Test
    void reorderStagesIncompleteStageListTest() {

        UUID sectionId = UUID.randomUUID();

        List<StageEntity> stageEntities = List.of(
                buildStageEntity(sectionId, 0),
                buildStageEntity(sectionId, 1),
                buildStageEntity(sectionId, 2),
                buildStageEntity(sectionId, 3)
        );

        SectionEntity sectionEntity = SectionEntity.builder()
                .id(sectionId)
                .name("Work-Path 1")
                .chapterId(UUID.randomUUID())
                .stages(Set.copyOf(stageEntities))
                .build();

        List<UUID> sortedStageIds = stageEntities.subList(0, 2)
                .stream()
                .map(StageEntity::getId)
                .sorted()
                .toList();


        StageOrderInput stageOrderInput = StageOrderInput.builder()
                .setSectionId(sectionId)
                .setStageIds(sortedStageIds)
                .build();

        //mock database
        when(sectionRepository.getReferenceById(stageOrderInput.getSectionId())).thenReturn(sectionEntity);
        when(sectionRepository.save(any())).thenReturn(sectionEntity);

        assertThrows(EntityNotFoundException.class, () -> sectionService.reorderStages(stageOrderInput));

    }

    @Test
    void getSectionsByChapterId() {
        UUID chapterId = UUID.randomUUID();
        List<SectionEntity> sectionEntities = List.of(
                SectionEntity.builder()
                        .id(UUID.randomUUID())
                        .name("Section 1")
                        .chapterId(chapterId)
                        .stages(new HashSet<>())
                        .build(),
                SectionEntity.builder()
                        .id(UUID.randomUUID())
                        .name("Section 1")
                        .chapterId(chapterId)
                        .stages(new HashSet<>())
                        .build()
        );

        List<Section> expectedResult = sectionEntities.stream().map(sectionMapper::entityToDto).toList();

        // mock database
        when(sectionRepository.findSectionEntitiesByChapterId(chapterId)).thenReturn(sectionEntities);

        List<Section> result = sectionService.getSectionByChapterId(chapterId);

        assertEquals(expectedResult, result);
    }

    private StageEntity buildStageEntity(UUID sectionId, int pos) {
        return StageEntity.builder()
                .id(UUID.randomUUID())
                .sectionId(sectionId)
                .position(pos)
                .requiredContents(new HashSet<>())
                .optionalContent(new HashSet<>())
                .build();
    }
}