package de.unistuttgart.iste.gits.content_service.service;

import de.unistuttgart.iste.gits.content_service.persistence.dao.ContentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.dao.ContentMetadataEmbeddable;
import de.unistuttgart.iste.gits.content_service.persistence.dao.SectionEntity;
import de.unistuttgart.iste.gits.content_service.persistence.dao.StageEntity;
import de.unistuttgart.iste.gits.content_service.persistence.mapper.ContentMapper;
import de.unistuttgart.iste.gits.content_service.persistence.mapper.StageMapper;
import de.unistuttgart.iste.gits.content_service.persistence.repository.ContentRepository;
import de.unistuttgart.iste.gits.content_service.persistence.repository.SectionRepository;
import de.unistuttgart.iste.gits.content_service.persistence.repository.StageRepository;
import de.unistuttgart.iste.gits.generated.dto.ContentType;
import de.unistuttgart.iste.gits.generated.dto.CreateStageInput;
import de.unistuttgart.iste.gits.generated.dto.Stage;
import de.unistuttgart.iste.gits.generated.dto.UpdateStageInput;
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
        CreateStageInput stageInput = CreateStageInput.builder().setOptionalContents(new ArrayList<>()).setRequiredContents(new ArrayList<>()).build();

        SectionEntity sectionEntity = SectionEntity.builder().id(UUID.randomUUID()).name("Test Section").stages(new HashSet<>()).chapterId(UUID.randomUUID()).build();

        StageEntity stageEntity = StageEntity.builder().sectionId(sectionEntity.getId()).position(0).optionalContents(new HashSet<>()).requiredContents(new HashSet<>()).build();

        //mock repository
        when(sectionRepository.existsById(sectionEntity.getId())).thenReturn(true);
        when(sectionRepository.getReferenceById(sectionEntity.getId())).thenReturn(sectionEntity);
        when(stageRepository.save(any())).thenReturn(stageEntity);


        //execute method under test
        Stage result = stageService.createNewStage(sectionEntity.getId(), stageInput);

        assertEquals(0, result.getPosition());
        assertTrue(result.getRequiredContents().isEmpty());
        assertTrue(result.getOptionalContents().isEmpty());

    }

    @Test
    void createNewStageTestWithContent() {
        UUID chapterId = UUID.randomUUID();
        List<ContentEntity> expectedReqContents = List.of(
                buildContentEntity(chapterId),
                buildContentEntity(chapterId));
        List<ContentEntity> expectedOptContents = List.of(
                buildContentEntity(chapterId),
                buildContentEntity(chapterId)
        );

        CreateStageInput stageInput = CreateStageInput.builder()
                .setRequiredContents(
                        expectedReqContents.stream()
                                .map(content -> content.getId())
                                .toList()
                ).setOptionalContents(
                        expectedOptContents.stream()
                                .map(content -> content.getId())
                                .toList()
                ).build();

        SectionEntity sectionEntity = SectionEntity.builder()
                .id(UUID.randomUUID())
                .name("Test Section")
                .stages(new HashSet<>())
                .chapterId(chapterId)
                .build();

        StageEntity stageEntity = StageEntity.builder()
                .sectionId(sectionEntity.getId())
                .position(0)
                .requiredContents(Set.copyOf(expectedReqContents))
                .optionalContents(Set.copyOf(expectedOptContents))
                .build();

        //mock repository
        when(sectionRepository.existsById(sectionEntity.getId())).thenReturn(true);
        when(sectionRepository.getReferenceById(sectionEntity.getId())).thenReturn(sectionEntity);
        when(sectionRepository.existsById(any())).thenReturn(true);
        when(contentRepository.findContentEntitiesByIdIn(stageInput.getRequiredContents())).thenReturn(expectedReqContents);
        when(contentRepository.findContentEntitiesByIdIn(stageInput.getOptionalContents())).thenReturn(expectedOptContents);
        when(stageRepository.save(any())).thenReturn(stageEntity);


        //execute method under test
        Stage result = stageService.createNewStage(sectionEntity.getId(), stageInput);

        assertEquals(0, result.getPosition());
        assertEquals(2, result.getRequiredContents().size());
        assertEquals(2, result.getOptionalContents().size());

    }

    @Test
    void createNewStageWithInvalidSectionId() {
        UUID id = UUID.randomUUID();
        CreateStageInput stageInput = CreateStageInput.builder().setOptionalContents(new ArrayList<>()).setRequiredContents(new ArrayList<>()).build();


        SectionEntity sectionEntity = SectionEntity.builder().id(id).name("Test Section").stages(new HashSet<>()).chapterId(UUID.randomUUID()).build();

        when(sectionRepository.existsById(sectionEntity.getId())).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> stageService.createNewStage(id, stageInput));
        assertThrows(NullPointerException.class, () -> stageService.createNewStage(null, stageInput));
    }

    @Test
    void updateStageTest() {
        //init data
        UUID chapterId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();

        List<ContentEntity> expectedReqContents = List.of(
                buildContentEntity(chapterId) ,
                buildContentEntity(chapterId));
        List<ContentEntity> expectedOptContents = List.of(
                buildContentEntity(chapterId),
                buildContentEntity(chapterId)
        );

        StageEntity oldStageEntity = StageEntity.builder()
                .id(stageId)
                .position(0)
                .sectionId(sectionId)
                .requiredContents(Set.of(expectedReqContents.get(0)))
                .optionalContents(new HashSet<>())
                .build();

        StageEntity expectedStageEntity = StageEntity.builder()
                .id(stageId)
                .position(0)
                .sectionId(sectionId)
                .requiredContents(Set.copyOf(expectedReqContents))
                .optionalContents(Set.copyOf(expectedOptContents))
                .build();

        Stage expectedResult = stageMapper.entityToDto(expectedStageEntity);

        SectionEntity sectionEntity = SectionEntity.builder()
                .id(sectionId)
                .name("Test1")
                .chapterId(chapterId)
                .stages(Set.of(oldStageEntity))
                .build();

        UpdateStageInput input = UpdateStageInput.builder()
                .setId(stageId)
                .setRequiredContents(
                        expectedReqContents.stream()
                                .map( content -> content.getId())
                                .toList()
                ).setOptionalContents(
                        expectedOptContents.stream()
                                .map( content -> content.getId())
                                .toList()
                ).build();

        //mock database
        when(stageRepository.existsById(any())).thenReturn(true);
        when(stageRepository.getReferenceById(input.getId())).thenReturn(oldStageEntity);
        when(sectionRepository.existsById(any())).thenReturn(true);
        when(sectionRepository.getReferenceById(oldStageEntity.getSectionId())).thenReturn(sectionEntity);
        when(contentRepository.findContentEntitiesByIdIn(input.getRequiredContents())).thenReturn(expectedReqContents);
        when(contentRepository.findContentEntitiesByIdIn(input.getOptionalContents())).thenReturn(expectedOptContents);
        when(stageRepository.save(any())).thenReturn(oldStageEntity);

        //execute method under test
        Stage result = stageService.updateStage(input);


        assertEquals(expectedStageEntity.getId(), result.getId());
        assertEquals(expectedStageEntity.getPosition(), result.getPosition());

        assertEquals(2, result.getRequiredContents().size());
        assertEquals(2, result.getOptionalContents().size());

        //assertEquals(expectedResult, result);

    }

    @Test
    void updateStageMissingStageTest(){
        //invalid Stage ID
        UpdateStageInput input = UpdateStageInput.builder()
                .setId(UUID.randomUUID())
                .setRequiredContents(new ArrayList<>())
                .setOptionalContents(new ArrayList<>())
                .build();

        //mock database
        when(stageRepository.existsById(any())).thenReturn(false);

        //execute method under test
        assertThrows(EntityNotFoundException.class, () -> stageService.updateStage(input));

    }

    @Test
    void updateStageInvalidSectionTest(){
        //invalid Section ID
        UpdateStageInput input = UpdateStageInput.builder()
                .setId(UUID.randomUUID())
                .setRequiredContents(new ArrayList<>())
                .setOptionalContents(new ArrayList<>())
                .build();
        StageEntity oldStageEntity = StageEntity.builder()
                .id(UUID.randomUUID())
                .position(0)
                .sectionId(UUID.randomUUID())
                .requiredContents(new HashSet<>())
                .optionalContents(new HashSet<>())
                .build();

        //mock database
        when(stageRepository.existsById(any())).thenReturn(true);
        when(stageRepository.getReferenceById(input.getId())).thenReturn(oldStageEntity);
        when(sectionRepository.existsById(any())).thenReturn(false);

        //execute method under test
        assertThrows(EntityNotFoundException.class, () -> stageService.updateStage(input));
    }

    @Test
    void updateStageMixedChapterIds(){
        // content with wrong chapter ID
        //init data
        UUID chapterId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID stageId = UUID.randomUUID();

        List<ContentEntity> expectedReqContents = List.of(
                buildContentEntity(chapterId) ,
                buildContentEntity(chapterId));
        List<ContentEntity> expectedOptContents = List.of(
                buildContentEntity(chapterId),
                buildContentEntity(UUID.randomUUID())
        );

        StageEntity oldStageEntity = StageEntity.builder()
                .id(stageId)
                .position(0)
                .sectionId(sectionId)
                .requiredContents(Set.of(expectedReqContents.get(0)))
                .optionalContents(new HashSet<>())
                .build();

        StageEntity expectedStageEntity = StageEntity.builder()
                .id(stageId)
                .position(0)
                .sectionId(sectionId)
                .requiredContents(Set.copyOf(expectedReqContents))
                .optionalContents(Set.of(expectedOptContents.get(0)))
                .build();



        SectionEntity sectionEntity = SectionEntity.builder()
                .id(sectionId)
                .name("Test1")
                .chapterId(chapterId)
                .stages(Set.of(oldStageEntity))
                .build();

        UpdateStageInput input = UpdateStageInput.builder()
                .setId(stageId)
                .setRequiredContents(
                        expectedReqContents.stream()
                                .map( content -> content.getId())
                                .toList()
                ).setOptionalContents(
                        expectedOptContents.stream()
                                .map( content -> content.getId())
                                .toList()
                ).build();

        //mock database
        when(stageRepository.existsById(any())).thenReturn(true);
        when(stageRepository.getReferenceById(input.getId())).thenReturn(oldStageEntity);
        when(sectionRepository.existsById(any())).thenReturn(true);
        when(sectionRepository.getReferenceById(oldStageEntity.getSectionId())).thenReturn(sectionEntity);
        when(contentRepository.findContentEntitiesByIdIn(input.getRequiredContents())).thenReturn(expectedReqContents);
        when(contentRepository.findContentEntitiesByIdIn(input.getOptionalContents())).thenReturn(expectedOptContents);
        when(stageRepository.save(any())).thenReturn(oldStageEntity);

        //execute method under test
        Stage result = stageService.updateStage(input);
        Stage expectedResult = stageMapper.entityToDto(expectedStageEntity);

        assertEquals(expectedStageEntity.getId(), result.getId());
        assertEquals(expectedStageEntity.getPosition(), result.getPosition());

        assertEquals(2, result.getRequiredContents().size());
        assertEquals(1, result.getOptionalContents().size());

        //assertEquals(expectedResult, result);
    }

    @Test
    void deleteStageTest() {
        //init
        UUID sectionId = UUID.randomUUID();
        Set<StageEntity> stageEntities = new HashSet<>();


        StageEntity deletedEntity = buildStageEntity(sectionId, 1);

        stageEntities.addAll(Set.of(
                buildStageEntity(sectionId, 0),
                deletedEntity,
                buildStageEntity(sectionId, 2),
                buildStageEntity(sectionId, 3)
        ));


        SectionEntity sectionEntity = SectionEntity.builder()
                .id(sectionId)
                .name("Test123")
                .chapterId(UUID.randomUUID())
                .stages(stageEntities)
                .build();

        //mock database
        when(stageRepository.existsById(any())).thenReturn(true);
        when(stageRepository.getReferenceById(deletedEntity.getId())).thenReturn(deletedEntity);
        when(sectionRepository.getReferenceById(sectionId)).thenReturn(sectionEntity);
        doNothing().when(stageRepository).delete(deletedEntity);

        UUID result = stageService.deleteStage(deletedEntity.getId());

        verify(stageRepository, times(1)).delete(deletedEntity);
        verify(sectionRepository, times(1)).save(sectionEntity);

        assertEquals(deletedEntity.getId(), result);
    }

    @Test
    void deleteStageInvalidIdTest(){

        //mock database
        when(stageRepository.existsById(any())).thenReturn(false);

        UUID uuid = UUID.randomUUID();

        assertThrows(EntityNotFoundException.class, () -> stageService.deleteStage(uuid));
    }

    private ContentEntity buildContentEntity(UUID chapterId){
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

    private StageEntity buildStageEntity (UUID sectionId, int pos){
        return StageEntity.builder()
                .id(UUID.randomUUID())
                .sectionId(sectionId)
                .position(pos)
                .requiredContents(new HashSet<>())
                .optionalContents(new HashSet<>())
                .build();
    }
}