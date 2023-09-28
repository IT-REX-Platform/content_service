package de.unistuttgart.iste.gits.content_service.service;

import de.unistuttgart.iste.gits.common.event.*;
import de.unistuttgart.iste.gits.common.exception.IncompleteEventMessageException;
import de.unistuttgart.iste.gits.content_service.TestData;
import de.unistuttgart.iste.gits.content_service.dapr.TopicPublisher;
import de.unistuttgart.iste.gits.content_service.persistence.entity.*;
import de.unistuttgart.iste.gits.content_service.persistence.mapper.ContentMapper;
import de.unistuttgart.iste.gits.content_service.persistence.repository.*;
import de.unistuttgart.iste.gits.content_service.test_config.MockTopicPublisherConfiguration;
import de.unistuttgart.iste.gits.content_service.validation.ContentValidator;
import de.unistuttgart.iste.gits.generated.dto.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.test.context.ContextConfiguration;

import java.time.OffsetDateTime;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = MockTopicPublisherConfiguration.class)
class ContentServiceTest {

    private final ContentRepository contentRepository = Mockito.mock(ContentRepository.class);
    private final SectionRepository sectionRepository = Mockito.mock(SectionRepository.class);
    private final StageService stageService = Mockito.mock(StageService.class);
    private final ContentMapper contentMapper = new ContentMapper(new ModelMapper());
    private final ContentValidator contentValidator = Mockito.spy(ContentValidator.class);
    private final TopicPublisher mockPublisher = Mockito.mock(TopicPublisher.class);
    private final UserProgressDataRepository userProgressDataRepository = Mockito.mock(UserProgressDataRepository.class);

    private final ContentService contentService = new ContentService(contentRepository, sectionRepository, userProgressDataRepository,
            stageService, contentMapper, contentValidator, mockPublisher);

    @Test
    void forwardResourceUpdates() {

        ResourceUpdateEvent dto = ResourceUpdateEvent.builder()
                .entityId(UUID.randomUUID())
                .contentIds(List.of(UUID.randomUUID()))
                .operation(CrudOperation.CREATE)
                .build();

        ContentEntity testEntity = ContentEntity.builder()
                .id(dto.getContentIds()
                        .get(0))
                .metadata(ContentMetadataEmbeddable.builder()
                        .chapterId(UUID.randomUUID())
                        .name("Test")
                        .rewardPoints(10)
                        .type(ContentType.MEDIA)
                        .suggestedDate(OffsetDateTime.now())
                        .build()
                )
                .build();

        //mock repository
        when(contentRepository.findAllById(dto.getContentIds())).thenReturn(List.of(testEntity));

        // execute method under test
        assertDoesNotThrow(() -> contentService.forwardResourceUpdates(dto));


        verify(mockPublisher, times(1))
                .forwardChange(dto.getEntityId(), List.of(testEntity.getMetadata().getChapterId()), dto.getOperation());
    }

    @Test
    void forwardFaultyResourceUpdates() {
        ResourceUpdateEvent noEntityDto = ResourceUpdateEvent.builder()
                .contentIds(List.of(UUID.randomUUID()))
                .operation(CrudOperation.CREATE)
                .build();
        ResourceUpdateEvent nullListDto = ResourceUpdateEvent.builder()
                .entityId(UUID.randomUUID())
                .operation(CrudOperation.CREATE)
                .build();
        ResourceUpdateEvent emptyListDto = ResourceUpdateEvent.builder()
                .entityId(UUID.randomUUID())
                .contentIds(new ArrayList<UUID>())
                .operation(CrudOperation.CREATE)
                .build();
        ResourceUpdateEvent noOperationDto = ResourceUpdateEvent.builder()
                .entityId(UUID.randomUUID())
                .contentIds(List.of(UUID.randomUUID()))
                .build();

        //execute method under test
        assertThrows(IncompleteEventMessageException.class, () -> contentService.forwardResourceUpdates(noEntityDto));
        assertThrows(IncompleteEventMessageException.class, () -> contentService.forwardResourceUpdates(nullListDto));
        assertThrows(IncompleteEventMessageException.class, () -> contentService.forwardResourceUpdates(noOperationDto));
    }

    @Test
    void cascadeContentDeletion() {

        ChapterChangeEvent dto = ChapterChangeEvent.builder()
                .chapterIds(List.of(UUID.randomUUID(), UUID.randomUUID()))
                .operation(CrudOperation.DELETE)
                .build();

        ContentEntity testEntity = ContentEntity.builder()
                .id(UUID.randomUUID())
                .metadata(ContentMetadataEmbeddable.builder()
                        .chapterId(dto.getChapterIds()
                                .get(0))
                        .name("Test")
                        .rewardPoints(10)
                        .type(ContentType.MEDIA)
                        .suggestedDate(OffsetDateTime.now())
                        .build()
                )
                .build();
        ContentEntity testEntity2 = ContentEntity.builder()
                .id(UUID.randomUUID())
                .metadata(ContentMetadataEmbeddable.builder()
                        .chapterId(dto.getChapterIds()
                                .get(1))
                        .name("Test2")
                        .rewardPoints(10)
                        .type(ContentType.FLASHCARDS)
                        .suggestedDate(OffsetDateTime.now())
                        .build()
                )
                .build();

        //mock repository
        when(contentRepository.findByChapterIdIn(dto.getChapterIds())).thenReturn(List.of(testEntity, testEntity2));
        Mockito.doNothing().when(contentRepository).delete(any(ContentEntity.class));

        //execute method under test
        assertDoesNotThrow(() -> contentService.cascadeContentDeletion(dto));

        verify(contentRepository, times(1)).delete(argThat(content -> content.getId().equals(testEntity.getId())));
        verify(contentRepository, times(1)).delete(argThat(content -> content.getId().equals(testEntity2.getId())));
        verify(mockPublisher, times(2)).notifyChange(any(ContentEntity.class), eq(CrudOperation.DELETE));
        verify(mockPublisher, times(1)).informContentDependentServices(List.of(testEntity.getId(), testEntity2.getId()), CrudOperation.DELETE);
        verify(userProgressDataRepository, times(1)).deleteByContentId(argThat(content -> content.equals(testEntity.getId())));
        verify(userProgressDataRepository, times(1)).deleteByContentId(argThat(content -> content.equals(testEntity2.getId())));
    }

    @Test
    void testFaultyCascadeContentDeletion(){
        ChapterChangeEvent wrongOperatorDto = ChapterChangeEvent.builder()
                .chapterIds(List.of(UUID.randomUUID()))
                .operation(CrudOperation.CREATE)
                .build();
        ChapterChangeEvent emptyListDto = ChapterChangeEvent.builder()
                .chapterIds(new ArrayList<UUID>())
                .operation(CrudOperation.DELETE)
                .build();
        ChapterChangeEvent nullListDto = ChapterChangeEvent.builder()
                .operation(CrudOperation.DELETE)
                .build();
        ChapterChangeEvent noOperationDto = ChapterChangeEvent.builder()
                .chapterIds(new ArrayList<UUID>())
                .build();

        //execute method under test
        assertDoesNotThrow(() -> contentService.cascadeContentDeletion(wrongOperatorDto));

        // ends before any DB access is made
        verify(contentRepository, times(0)).findByChapterIdIn(any());
        verify(contentRepository, times(0)).delete(any(ContentEntity.class));
        verify(mockPublisher, times(0)).informContentDependentServices(any(), any());


        //execute method under test
        assertThrows(IncompleteEventMessageException.class, () -> contentService.cascadeContentDeletion(nullListDto));
        assertThrows(IncompleteEventMessageException.class, () -> contentService.cascadeContentDeletion(noOperationDto));
    }

    @Test
    void testSkillTypesByChapterId() {
        UUID chapterId1 = UUID.randomUUID();
        UUID chapterId2 = UUID.randomUUID();

        when(contentRepository.findSkillTypesByChapterId(chapterId1)).thenReturn(
                List.of(List.of(SkillType.REMEMBER), List.of(SkillType.UNDERSTAND, SkillType.REMEMBER))
        );
        when(contentRepository.findSkillTypesByChapterId(chapterId2)).thenReturn(
                List.of(List.of(SkillType.APPLY, SkillType.REMEMBER))
        );

        var actualSkillTypes = contentService.getAchievableSkillTypesByChapterIds(List.of(chapterId1, chapterId2));

        assertThat(actualSkillTypes, contains(
                containsInAnyOrder(SkillType.REMEMBER, SkillType.UNDERSTAND),
                containsInAnyOrder(SkillType.REMEMBER, SkillType.APPLY)
        ));
    }

    @Test
    void testSkillTypesByChapterIdNoSkillTypes() {
        UUID chapterId = UUID.randomUUID();

        when(contentRepository.findSkillTypesByChapterId(chapterId)).thenReturn(
                List.of(List.of())
        );

        var actualSkillTypes = contentService.getAchievableSkillTypesByChapterIds(List.of(chapterId));

        assertThat(actualSkillTypes, contains(is(empty())));
    }

    @Test
    void getContentWithNoSection() {
        UUID chapterId = UUID.randomUUID();
        UUID chapterId2 = UUID.randomUUID();
        UUID chapterId3 = UUID.randomUUID();
        List<UUID> chapterIds = List.of(chapterId, chapterId2, chapterId3);

        UUID sectionId = UUID.randomUUID();
        UUID sectionId2 = UUID.randomUUID();
        UUID sectionId3 = UUID.randomUUID();

        List<ContentEntity> mediaContentEntities = List.of(
                TestData.buildContentEntity(chapterId),
                TestData.buildContentEntity(chapterId),
                TestData.buildContentEntity(chapterId),
                TestData.buildContentEntity(chapterId),
                TestData.buildContentEntity(chapterId2),
                TestData.buildContentEntity(chapterId3)
        );

        //case: chapter has both linked and unlinked content
        SectionEntity sectionEntity = SectionEntity.builder()
                .chapterId(chapterId)
                .name("Test Section")
                .id(sectionId)
                .stages(Set.of(buildDummyStage().requiredContents(Set.of(mediaContentEntities.get(0))).optionalContents(Set.of(mediaContentEntities.get(1))).build()))
                .build();

        //case: chapter has only unlinked content
        SectionEntity sectionEntity2 = SectionEntity.builder()
                .chapterId(chapterId2)
                .name("Test Section 2")
                .id(sectionId2)
                .stages(Set.of(buildDummyStage().requiredContents(new HashSet<>()).optionalContents(new HashSet<>()).build()))
                .build();

        //case: chapter has only linked content
        SectionEntity sectionEntity3 = SectionEntity.builder()
                .chapterId(chapterId2)
                .name("Test Section 3")
                .id(sectionId3)
                .stages(Set.of(buildDummyStage().requiredContents(Set.of(mediaContentEntities.get(5))).optionalContents(new HashSet<>()).build()))
                .build();

        // expected outcome
        List<Content> unlinkedContentForChapter1 = mediaContentEntities.subList(2, 4).stream().map(contentMapper::entityToDto).toList();
        List<Content> unlinkedContentForChapter2 = mediaContentEntities.subList(4, 5).stream().map(contentMapper::entityToDto).toList();

        //mock database queries
        when(sectionRepository.findByChapterIdInOrderByPosition(chapterIds)).thenReturn(List.of(sectionEntity, sectionEntity2, sectionEntity3));
        when(contentRepository.findByChapterIdIn(chapterIds)).thenReturn(mediaContentEntities);

        // execute method under test
        List<List<Content>> result = contentService.getContentWithNoSection(chapterIds);

        System.out.println(mediaContentEntities);
        System.out.println(mediaContentEntities);

        // compare expected vs actual outcome
        assertEquals(unlinkedContentForChapter1, result.get(0));
        assertEquals(unlinkedContentForChapter2, result.get(1));
        assertTrue(result.get(2).isEmpty());

    }

    private StageEntity.StageEntityBuilder buildDummyStage() {
        return StageEntity.builder().id(UUID.randomUUID()).position(0);
    }
}