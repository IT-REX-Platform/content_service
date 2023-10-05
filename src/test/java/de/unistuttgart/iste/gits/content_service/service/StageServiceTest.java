package de.unistuttgart.iste.gits.content_service.service;

import de.unistuttgart.iste.gits.content_service.persistence.entity.*;
import de.unistuttgart.iste.gits.content_service.persistence.mapper.ContentMapper;
import de.unistuttgart.iste.gits.content_service.persistence.mapper.StageMapper;
import de.unistuttgart.iste.gits.content_service.persistence.repository.*;
import de.unistuttgart.iste.gits.generated.dto.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StageServiceTest {

    private final StageRepository stageRepository = Mockito.mock(StageRepository.class);
    private final SectionRepository sectionRepository = Mockito.mock(SectionRepository.class);

    private final ContentRepository contentRepository = Mockito.mock(ContentRepository.class);

    private final StageMapper stageMapper = new StageMapper(new ContentMapper(new ModelMapper()));

    private final StageService stageService = new StageService(
            stageRepository,
            sectionRepository,
            contentRepository,
            stageMapper);

    @Test
    void createNewStageTest() {
        final CreateStageInput stageInput = CreateStageInput.builder()
                .setOptionalContents(new ArrayList<>())
                .setRequiredContents(new ArrayList<>()).build();

        final SectionEntity sectionEntity = SectionEntity.builder()
                .id(UUID.randomUUID())
                .name("Test Section")
                .stages(new HashSet<>())
                .chapterId(UUID.randomUUID())
                .build();

        final StageEntity stageEntity = StageEntity.builder()
                .sectionId(sectionEntity.getId())
                .position(0)
                .optionalContents(new HashSet<>())
                .requiredContents(new HashSet<>())
                .build();

        //mock repository
        when(sectionRepository.existsById(sectionEntity.getId())).thenReturn(true);
        when(sectionRepository.findById(sectionEntity.getId())).thenReturn(Optional.of(sectionEntity));
        when(stageRepository.save(any())).thenReturn(stageEntity);


        //execute method under test
        final Stage result = stageService.createNewStage(sectionEntity.getId(), stageInput);

        assertEquals(0, result.getPosition());
        assertTrue(result.getRequiredContents().isEmpty());
        assertTrue(result.getOptionalContents().isEmpty());

    }

    @Test
    void createNewStageTestWithContent() {
        final UUID chapterId = UUID.randomUUID();
        final List<ContentEntity> expectedReqContents = List.of(
                buildContentEntity(chapterId),
                buildContentEntity(chapterId));
        final List<ContentEntity> expectedOptContents = List.of(
                buildContentEntity(chapterId),
                buildContentEntity(chapterId)
        );

        final CreateStageInput stageInput = CreateStageInput.builder()
                .setRequiredContents(
                        expectedReqContents.stream()
                                .map(ContentEntity::getId)
                                .toList()
                ).setOptionalContents(
                        expectedOptContents.stream()
                                .map(ContentEntity::getId)
                                .toList()
                ).build();

        final SectionEntity sectionEntity = SectionEntity.builder()
                .id(UUID.randomUUID())
                .name("Test Section")
                .stages(new HashSet<>())
                .chapterId(chapterId)
                .build();

        final StageEntity stageEntity = StageEntity.builder()
                .sectionId(sectionEntity.getId())
                .position(0)
                .requiredContents(Set.copyOf(expectedReqContents))
                .optionalContents(Set.copyOf(expectedOptContents))
                .build();

        //mock repository
        when(sectionRepository.findById(sectionEntity.getId())).thenReturn(Optional.of(sectionEntity));
        when(sectionRepository.existsById(any())).thenReturn(true);
        when(contentRepository.findAllById(stageInput.getRequiredContents())).thenReturn(expectedReqContents);
        when(contentRepository.findAllById(stageInput.getOptionalContents())).thenReturn(expectedOptContents);
        when(stageRepository.save(any())).thenReturn(stageEntity);


        //execute method under test
        final Stage result = stageService.createNewStage(sectionEntity.getId(), stageInput);

        assertEquals(0, result.getPosition());
        assertEquals(2, result.getRequiredContents().size());
        assertEquals(2, result.getOptionalContents().size());

    }

    @Test
    void createNewStageWithInvalidSectionId() {
        final UUID id = UUID.randomUUID();
        final CreateStageInput stageInput = CreateStageInput.builder().setOptionalContents(new ArrayList<>()).setRequiredContents(new ArrayList<>()).build();


        final SectionEntity sectionEntity = SectionEntity.builder().id(id).name("Test Section").stages(new HashSet<>()).chapterId(UUID.randomUUID()).build();

        when(sectionRepository.findById(sectionEntity.getId())).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> stageService.createNewStage(id, stageInput));
    }

    @Test
    void updateStageTest() {
        //init data
        final UUID chapterId = UUID.randomUUID();
        final UUID sectionId = UUID.randomUUID();
        final UUID stageId = UUID.randomUUID();

        final List<ContentEntity> expectedReqContents = List.of(
                buildContentEntity(chapterId) ,
                buildContentEntity(chapterId));
        final List<ContentEntity> expectedOptContents = List.of(
                buildContentEntity(chapterId),
                buildContentEntity(chapterId)
        );

        final StageEntity oldStageEntity = StageEntity.builder()
                .id(stageId)
                .position(0)
                .sectionId(sectionId)
                .requiredContents(Set.of(expectedReqContents.get(0)))
                .optionalContents(new HashSet<>())
                .build();

        final StageEntity expectedStageEntity = StageEntity.builder()
                .id(stageId)
                .position(0)
                .sectionId(sectionId)
                .requiredContents(Set.copyOf(expectedReqContents))
                .optionalContents(Set.copyOf(expectedOptContents))
                .build();

        final Stage expectedResult = stageMapper.entityToDto(expectedStageEntity);

        final SectionEntity sectionEntity = SectionEntity.builder()
                .id(sectionId)
                .name("Test1")
                .chapterId(chapterId)
                .stages(Set.of(oldStageEntity))
                .build();

        final UpdateStageInput input = UpdateStageInput.builder()
                .setId(stageId)
                .setRequiredContents(
                        expectedReqContents.stream()
                                .map(ContentEntity::getId)
                                .toList()
                ).setOptionalContents(
                        expectedOptContents.stream()
                                .map(ContentEntity::getId)
                                .toList()
                ).build();

        //mock database
        when(stageRepository.findById(input.getId())).thenReturn(Optional.of(oldStageEntity));
        when(sectionRepository.findById(oldStageEntity.getSectionId())).thenReturn(Optional.of(sectionEntity));
        when(contentRepository.findAllById(input.getRequiredContents())).thenReturn(expectedReqContents);
        when(contentRepository.findAllById(input.getOptionalContents())).thenReturn(expectedOptContents);
        when(stageRepository.save(any())).thenReturn(oldStageEntity);

        //execute method under test
        final Stage result = stageService.updateStage(input);


        assertEquals(expectedStageEntity.getId(), result.getId());
        assertEquals(expectedStageEntity.getPosition(), result.getPosition());

        assertEquals(2, result.getRequiredContents().size());
        assertEquals(2, result.getOptionalContents().size());

    }

    @Test
    void updateStageMissingStageTest(){
        //invalid Stage ID
        final UpdateStageInput input = UpdateStageInput.builder()
                .setId(UUID.randomUUID())
                .setRequiredContents(new ArrayList<>())
                .setOptionalContents(new ArrayList<>())
                .build();

        //mock database
        when(stageRepository.findById(any())).thenReturn(Optional.empty());

        //execute method under test
        assertThrows(EntityNotFoundException.class, () -> stageService.updateStage(input));

    }

    @Test
    void updateStageInvalidSectionTest(){
        //invalid Section ID
        final UpdateStageInput input = UpdateStageInput.builder()
                .setId(UUID.randomUUID())
                .setRequiredContents(new ArrayList<>())
                .setOptionalContents(new ArrayList<>())
                .build();
        final StageEntity oldStageEntity = StageEntity.builder()
                .id(UUID.randomUUID())
                .position(0)
                .sectionId(UUID.randomUUID())
                .requiredContents(new HashSet<>())
                .optionalContents(new HashSet<>())
                .build();

        //mock database
        when(stageRepository.findById(input.getId())).thenReturn(Optional.of(oldStageEntity));
        when(sectionRepository.findById(any())).thenReturn(Optional.empty());

        //execute method under test
        assertThrows(EntityNotFoundException.class, () -> stageService.updateStage(input));
    }

    @Test
    void updateStageMixedChapterIds(){
        // content with wrong chapter ID
        //init data
        final UUID chapterId = UUID.randomUUID();
        final UUID sectionId = UUID.randomUUID();
        final UUID stageId = UUID.randomUUID();

        final List<ContentEntity> expectedReqContents = List.of(
                buildContentEntity(chapterId) ,
                buildContentEntity(chapterId));
        final List<ContentEntity> expectedOptContents = List.of(
                buildContentEntity(chapterId),
                buildContentEntity(UUID.randomUUID())
        );

        final StageEntity oldStageEntity = StageEntity.builder()
                .id(stageId)
                .position(0)
                .sectionId(sectionId)
                .requiredContents(Set.of(expectedReqContents.get(0)))
                .optionalContents(new HashSet<>())
                .build();

        final StageEntity expectedStageEntity = StageEntity.builder()
                .id(stageId)
                .position(0)
                .sectionId(sectionId)
                .requiredContents(Set.copyOf(expectedReqContents))
                .optionalContents(Set.of(expectedOptContents.get(0)))
                .build();



        final SectionEntity sectionEntity = SectionEntity.builder()
                .id(sectionId)
                .name("Test1")
                .chapterId(chapterId)
                .stages(Set.of(oldStageEntity))
                .build();

        final UpdateStageInput input = UpdateStageInput.builder()
                .setId(stageId)
                .setRequiredContents(
                        expectedReqContents.stream()
                                .map(ContentEntity::getId)
                                .toList()
                ).setOptionalContents(
                        expectedOptContents.stream()
                                .map(ContentEntity::getId)
                                .toList()
                ).build();

        //mock database
        when(stageRepository.findById(input.getId())).thenReturn(Optional.of(oldStageEntity));
        when(sectionRepository.findById(oldStageEntity.getSectionId())).thenReturn(Optional.of(sectionEntity));
        when(contentRepository.findAllById(input.getRequiredContents())).thenReturn(expectedReqContents);
        when(contentRepository.findAllById(input.getOptionalContents())).thenReturn(expectedOptContents);
        when(stageRepository.save(any())).thenReturn(oldStageEntity);

        //execute method under test
        final Stage result = stageService.updateStage(input);
        final Stage expectedResult = stageMapper.entityToDto(expectedStageEntity);

        assertEquals(expectedStageEntity.getId(), result.getId());
        assertEquals(expectedStageEntity.getPosition(), result.getPosition());

        assertEquals(2, result.getRequiredContents().size());
        assertEquals(1, result.getOptionalContents().size());
    }

    @Test
    void deleteStageTest() {
        //init
        final UUID sectionId = UUID.randomUUID();


        final StageEntity deletedEntity = buildStageEntity(sectionId, 1);

        final Set<StageEntity> stageEntities = new HashSet<>(Set.of(
                buildStageEntity(sectionId, 0),
                deletedEntity,
                buildStageEntity(sectionId, 2),
                buildStageEntity(sectionId, 3)
        ));


        final SectionEntity sectionEntity = SectionEntity.builder()
                .id(sectionId)
                .name("Test123")
                .chapterId(UUID.randomUUID())
                .stages(stageEntities)
                .build();

        //mock database
        when(stageRepository.findById(deletedEntity.getId())).thenReturn(Optional.of(deletedEntity));
        when(sectionRepository.getReferenceById(sectionId)).thenReturn(sectionEntity);
        doNothing().when(stageRepository).delete(deletedEntity);

        final UUID result = stageService.deleteStage(deletedEntity.getId());

        verify(stageRepository, times(1)).delete(deletedEntity);
        verify(sectionRepository, times(1)).save(sectionEntity);

        assertEquals(deletedEntity.getId(), result);
    }

    @Test
    void deleteStageInvalidIdTest(){

        //mock database
        when(stageRepository.existsById(any())).thenReturn(false);

        final UUID uuid = UUID.randomUUID();

        assertThrows(EntityNotFoundException.class, () -> stageService.deleteStage(uuid));
    }

    private ContentEntity buildContentEntity(final UUID chapterId){
        return ContentEntity.builder()
                .id(UUID.randomUUID())
                .metadata(
                        ContentMetadataEmbeddable.builder()
                                .tags(new HashSet<>())
                                .type(ContentType.MEDIA)
                                .suggestedDate(OffsetDateTime.now())
                                .rewardPoints(20)
                                .chapterId(chapterId)
                                .build()
                ).build();
    }

    private StageEntity buildStageEntity (final UUID sectionId, final int pos){
        return StageEntity.builder()
                .id(UUID.randomUUID())
                .sectionId(sectionId)
                .position(pos)
                .requiredContents(new HashSet<>())
                .optionalContents(new HashSet<>())
                .build();
    }
}