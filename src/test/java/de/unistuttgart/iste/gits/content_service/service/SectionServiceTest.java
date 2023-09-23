package de.unistuttgart.iste.gits.content_service.service;

import de.unistuttgart.iste.gits.common.event.ChapterChangeEvent;
import de.unistuttgart.iste.gits.common.event.CrudOperation;
import de.unistuttgart.iste.gits.common.exception.IncompleteEventMessageException;
import de.unistuttgart.iste.gits.content_service.persistence.entity.SectionEntity;
import de.unistuttgart.iste.gits.content_service.persistence.entity.StageEntity;
import de.unistuttgart.iste.gits.content_service.persistence.mapper.*;
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
        final CreateSectionInput input = CreateSectionInput.builder()
                .setChapterId(UUID.randomUUID())
                .setName("Test Section")
                .build();

        final SectionEntity sectionEntity = SectionEntity.builder()
                .name(input.getName())
                .id(UUID.randomUUID())
                .chapterId(input.getChapterId())
                .stages(new HashSet<>()).build();

        final Section expectedResult = Section.builder()
                .setId(sectionEntity.getId())
                .setName(sectionEntity.getName())
                .setChapterId(sectionEntity.getChapterId())
                .setStages(new ArrayList<>())
                .build();

        //mock database
        when(sectionRepository.save(any())).thenReturn(sectionEntity);

        // execute method under test
        final Section result = sectionService.createSection(input);

        assertEquals(expectedResult, result);
        assertEquals(expectedResult.getId(), result.getId());
        assertEquals(expectedResult.getName(), result.getName());
        assertEquals(expectedResult.getChapterId(), result.getChapterId());
        assertEquals(expectedResult.getStages(), result.getStages());
    }

    @Test
    void updateSectionTest() {
        final UUID sectionId = UUID.randomUUID();
        final String newName = "Test Section";

        final SectionEntity oldSectionEntity = SectionEntity.builder()
                .name("This is a Section")
                .id(sectionId)
                .chapterId(UUID.randomUUID())
                .stages(new HashSet<>()).build();

        final SectionEntity newSectionEntity = SectionEntity.builder()
                .name(newName)
                .id(sectionId)
                .chapterId(oldSectionEntity.getChapterId())
                .stages(new HashSet<>()).build();

        final Section expectedResult = Section.builder()
                .setId(newSectionEntity.getId())
                .setName(newSectionEntity.getName())
                .setChapterId(newSectionEntity.getChapterId())
                .setStages(new ArrayList<>())
                .build();

        //mock database
        when(sectionRepository.existsById(sectionId)).thenReturn(true);
        when(sectionRepository.getReferenceById(sectionId)).thenReturn(oldSectionEntity);
        when(sectionRepository.save(newSectionEntity)).thenReturn(newSectionEntity);

        // execute method under test
        final Section result = sectionService.updateSectionName(sectionId, newName);

        verify(sectionRepository, times(1)).save(newSectionEntity);

        assertEquals(expectedResult, result);
        assertEquals(expectedResult.getId(), result.getId());
        assertEquals(expectedResult.getName(), result.getName());
        assertEquals(expectedResult.getChapterId(), result.getChapterId());
        assertEquals(expectedResult.getStages(), result.getStages());
    }

    @Test
    void updateNoneExistingSectionTest() {
        final UUID sectionId = UUID.randomUUID();
        final String newName = "Test Section";

        //mock database
        when(sectionRepository.existsById(sectionId)).thenReturn(false);

        // execute method under test
        assertThrows(EntityNotFoundException.class, () -> sectionService.updateSectionName(sectionId, newName));
    }

    @Test
    void cascadeSectionDeletionWithValidData() {
        // Initialize a ChapterChangeEvent DTO with valid data
        final ChapterChangeEvent dto = ChapterChangeEvent.builder()
                .chapterIds(Collections.singletonList(UUID.randomUUID()))
                .operation(CrudOperation.DELETE)
                .build();

        // Create a list of SectionEntity objects to mock the repository's response
        final SectionEntity sectionEntity = SectionEntity.builder()
                .id(UUID.randomUUID())
                .chapterId(dto.getChapterIds().get(0))
                .build();

        // Mock the repository's behavior
        when(sectionRepository.findByChapterIdIn(dto.getChapterIds())).thenReturn(Collections.singletonList(sectionEntity));
        doNothing().when(sectionRepository).deleteAllInBatch(any());

        // Execute the method under test
        assertDoesNotThrow(() -> sectionService.cascadeSectionDeletion(dto));

        // Verify that the repository methods were called as expected
        verify(sectionRepository, times(1)).findByChapterIdIn(dto.getChapterIds());
        verify(sectionRepository, times(1)).deleteAllInBatch(Collections.singletonList(sectionEntity));
    }

    @Test
    void cascadeSectionDeletionWithIncompleteData() {
        // Initialize a ChapterChangeEvent DTO with incomplete data
        final ChapterChangeEvent dto = ChapterChangeEvent.builder()
                .chapterIds(Collections.emptyList())
                .operation(null)
                .build();

        // Execute the method under test and expect a NullPointerException
        assertThrows(IncompleteEventMessageException.class, () -> sectionService.cascadeSectionDeletion(dto));

        // Verify that the repository methods were not called
        verify(sectionRepository, never()).findByChapterIdIn(any());
        verify(sectionRepository, never()).deleteAllInBatch(any());
    }

    // case: update Section with existing Stages
    @Test
    void updateSectionWithStagesTest() {
        final UUID sectionId = UUID.randomUUID();
        final String newName = "Test Section";

        final SectionEntity oldSectionEntity = SectionEntity.builder()
                .name("This is a Section")
                .id(sectionId)
                .chapterId(UUID.randomUUID())
                .stages(
                        Set.of(
                                buildStageEntity(sectionId, 0),
                                buildStageEntity(sectionId, 1)
                        )
                ).build();

        final SectionEntity newSectionEntity = SectionEntity.builder()
                .name(newName)
                .id(sectionId)
                .chapterId(oldSectionEntity.getChapterId())
                .stages(oldSectionEntity.getStages())
                .build();


        final Section expectedResult = Section.builder()
                .setId(newSectionEntity.getId())
                .setName(newSectionEntity.getName())
                .setChapterId(newSectionEntity.getChapterId())
                .setStages(newSectionEntity.getStages().stream()
                        .map(stageMapper::entityToDto).sorted(Comparator.comparingInt(Stage::getPosition))
                        .toList())
                .build();

        //mock database
        when(sectionRepository.existsById(sectionId)).thenReturn(true);
        when(sectionRepository.getReferenceById(sectionId)).thenReturn(oldSectionEntity);
        when(sectionRepository.save(any())).thenReturn(newSectionEntity);

        // execute method under test
        final Section result = sectionService.updateSectionName(sectionId, newName);

        verify(sectionRepository, times(1)).save(newSectionEntity);

        assertEquals(expectedResult.getId(), result.getId());
        assertEquals(expectedResult.getName(), result.getName());
        assertEquals(expectedResult.getChapterId(), result.getChapterId());
        assertEquals(expectedResult.getStages(), result.getStages());
        assertEquals(expectedResult, result);
    }

    @Test
    void deleteSection() {
        final UUID input = UUID.randomUUID();

        //mock database
        when(sectionRepository.existsById(input)).thenReturn(true);
        doNothing().when(sectionRepository).deleteById(input);

        final UUID result = sectionService.deleteWorkPath(input);

        verify(sectionRepository, times(1)).deleteById(input);
        assertEquals(input, result);
    }

    @Test
    void deleteInvalidIdSection() {
        final UUID input = UUID.randomUUID();

        //mock database
        when(sectionRepository.existsById(input)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> sectionService.deleteWorkPath(input));
    }


    // case: valid input provided
    @Test
    void reorderStagesTest() {

        final UUID sectionId = UUID.randomUUID();

        final Set<StageEntity> stageEntities = Set.of(
                buildStageEntity(sectionId, 0),
                buildStageEntity(sectionId, 1),
                buildStageEntity(sectionId, 2),
                buildStageEntity(sectionId, 3)
        );

        final SectionEntity sectionEntity = SectionEntity.builder()
                .id(sectionId)
                .name("Section 1")
                .chapterId(UUID.randomUUID())
                .stages(stageEntities)
                .build();

        final List<UUID> sortedStageIds = stageEntities.stream().map(StageEntity::getId).sorted().toList();


        //mock database
        when(sectionRepository.getReferenceById(sectionId)).thenReturn(sectionEntity);
        when(sectionRepository.save(any())).thenReturn(sectionEntity);

        final Section result = sectionService.reorderStages(sectionId, sortedStageIds);

        verify(sectionRepository, times(1)).getReferenceById(sectionId);
        verify(sectionRepository, times(1)).save(any());

        for (final Stage stage : result.getStages()) {
            assertEquals(sortedStageIds.indexOf(stage.getId()), stage.getPosition());
        }
    }

    // case: received stage ID list contains elements that are not part of the Section
    @Test
    void reorderStagesInvalidStageListTest() {

        final UUID sectionId = UUID.randomUUID();

        final List<StageEntity> stageEntities = List.of(
                buildStageEntity(sectionId, 0),
                buildStageEntity(sectionId, 1),
                buildStageEntity(sectionId, 2),
                buildStageEntity(sectionId, 3)
        );

        final SectionEntity sectionEntity = SectionEntity.builder()
                .id(sectionId)
                .name("Section 1")
                .chapterId(UUID.randomUUID())
                .stages(Set.copyOf(stageEntities.subList(0, 2)))
                .build();

        final List<UUID> sortedStageIds = stageEntities.stream()
                .map(StageEntity::getId)
                .sorted()
                .toList();

        //mock database
        when(sectionRepository.getReferenceById(sectionId)).thenReturn(sectionEntity);
        when(sectionRepository.save(any())).thenReturn(sectionEntity);

        assertThrows(EntityNotFoundException.class, () -> sectionService.reorderStages(sectionId, sortedStageIds));

    }

    // case: received stage ID list is incomplete
    @Test
    void reorderStagesIncompleteStageListTest() {

        final UUID sectionId = UUID.randomUUID();

        final List<StageEntity> stageEntities = List.of(
                buildStageEntity(sectionId, 0),
                buildStageEntity(sectionId, 1),
                buildStageEntity(sectionId, 2),
                buildStageEntity(sectionId, 3)
        );

        final SectionEntity sectionEntity = SectionEntity.builder()
                .id(sectionId)
                .name("Work-Path 1")
                .chapterId(UUID.randomUUID())
                .stages(Set.copyOf(stageEntities))
                .build();

        final List<UUID> sortedStageIds = stageEntities.subList(0, 2)
                .stream()
                .map(StageEntity::getId)
                .sorted()
                .toList();

        //mock database
        when(sectionRepository.getReferenceById(sectionId)).thenReturn(sectionEntity);
        when(sectionRepository.save(any())).thenReturn(sectionEntity);

        assertThrows(EntityNotFoundException.class, () -> sectionService.reorderStages(sectionId, sortedStageIds));

    }

    private StageEntity buildStageEntity(final UUID sectionId, final int pos) {
        return StageEntity.builder()
                .id(UUID.randomUUID())
                .sectionId(sectionId)
                .position(pos)
                .requiredContents(new HashSet<>())
                .optionalContents(new HashSet<>())
                .build();
    }

}