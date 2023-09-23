package de.unistuttgart.iste.gits.content_service.service;

import de.unistuttgart.iste.gits.common.event.*;
import de.unistuttgart.iste.gits.common.exception.IncompleteEventMessageException;
import de.unistuttgart.iste.gits.content_service.dapr.TopicPublisher;
import de.unistuttgart.iste.gits.content_service.persistence.entity.ContentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.entity.ContentMetadataEmbeddable;
import de.unistuttgart.iste.gits.content_service.persistence.mapper.ContentMapper;
import de.unistuttgart.iste.gits.content_service.persistence.repository.ContentRepository;
import de.unistuttgart.iste.gits.content_service.persistence.repository.UserProgressDataRepository;
import de.unistuttgart.iste.gits.content_service.test_config.MockTopicPublisherConfiguration;
import de.unistuttgart.iste.gits.content_service.validation.ContentValidator;
import de.unistuttgart.iste.gits.generated.dto.ContentType;
import de.unistuttgart.iste.gits.generated.dto.SkillType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.test.context.ContextConfiguration;

import java.time.OffsetDateTime;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = MockTopicPublisherConfiguration.class)
class ContentServiceTest {

    private final ContentRepository contentRepository = Mockito.mock(ContentRepository.class);
    private final StageService stageService = Mockito.mock(StageService.class);
    private final ContentMapper contentMapper = new ContentMapper(new ModelMapper());
    private final ContentValidator contentValidator = Mockito.spy(ContentValidator.class);
    private final TopicPublisher mockPublisher = Mockito.mock(TopicPublisher.class);
    private final UserProgressDataRepository userProgressDataRepository = Mockito.mock(UserProgressDataRepository.class);

    private final ContentService contentService = new ContentService(contentRepository, userProgressDataRepository,
            stageService, contentMapper, contentValidator, mockPublisher);

    @Test
    void forwardResourceUpdates() {

        final ResourceUpdateEvent dto = ResourceUpdateEvent.builder()
                .entityId(UUID.randomUUID())
                .contentIds(List.of(UUID.randomUUID()))
                .operation(CrudOperation.CREATE)
                .build();

        final ContentEntity testEntity = ContentEntity.builder()
                .id(dto.getContentIds()
                        .get(0))
                .metadata( ContentMetadataEmbeddable.builder()
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
        final ResourceUpdateEvent noEntityDto = ResourceUpdateEvent.builder()
                .contentIds(List.of(UUID.randomUUID()))
                .operation(CrudOperation.CREATE)
                .build();
        final ResourceUpdateEvent nullListDto = ResourceUpdateEvent.builder()
                .entityId(UUID.randomUUID())
                .operation(CrudOperation.CREATE)
                .build();
        final ResourceUpdateEvent emptyListDto = ResourceUpdateEvent.builder()
                .entityId(UUID.randomUUID())
                .contentIds(new ArrayList<UUID>())
                .operation(CrudOperation.CREATE)
                .build();
        final ResourceUpdateEvent noOperationDto = ResourceUpdateEvent.builder()
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

        final ChapterChangeEvent dto = ChapterChangeEvent.builder()
                .chapterIds(List.of(UUID.randomUUID(), UUID.randomUUID()))
                .operation(CrudOperation.DELETE)
                .build();

        final ContentEntity testEntity = ContentEntity.builder()
                .id(UUID.randomUUID())
                .metadata( ContentMetadataEmbeddable.builder()
                        .chapterId(dto.getChapterIds()
                                .get(0))
                        .name("Test")
                        .rewardPoints(10)
                        .type(ContentType.MEDIA)
                        .suggestedDate(OffsetDateTime.now())
                        .build()
                )
                .build();
        final ContentEntity testEntity2 = ContentEntity.builder()
                .id(UUID.randomUUID())
                .metadata( ContentMetadataEmbeddable.builder()
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
        final ChapterChangeEvent wrongOperatorDto = ChapterChangeEvent.builder()
                .chapterIds(List.of(UUID.randomUUID()))
                .operation(CrudOperation.CREATE)
                .build();
        final ChapterChangeEvent emptyListDto = ChapterChangeEvent.builder()
                .chapterIds(new ArrayList<UUID>())
                .operation(CrudOperation.DELETE)
                .build();
        final ChapterChangeEvent nullListDto = ChapterChangeEvent.builder()
                .operation(CrudOperation.DELETE)
                .build();
        final ChapterChangeEvent noOperationDto = ChapterChangeEvent.builder()
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
        final UUID chapterId1 = UUID.randomUUID();
        final UUID chapterId2 = UUID.randomUUID();

        when(contentRepository.findSkillTypesByChapterId(chapterId1)).thenReturn(
                List.of(List.of(SkillType.REMEMBER), List.of(SkillType.UNDERSTAND, SkillType.REMEMBER))
        );
        when(contentRepository.findSkillTypesByChapterId(chapterId2)).thenReturn(
                List.of(List.of(SkillType.APPLY, SkillType.REMEMBER))
        );

        final var actualSkillTypes = contentService.getAchievableSkillTypesByChapterIds(List.of(chapterId1, chapterId2));

        assertThat(actualSkillTypes, contains(
                containsInAnyOrder(SkillType.REMEMBER, SkillType.UNDERSTAND),
                containsInAnyOrder(SkillType.REMEMBER, SkillType.APPLY)
        ));
    }

    @Test
    void testSkillTypesByChapterIdNoSkillTypes() {
        final UUID chapterId = UUID.randomUUID();

        when(contentRepository.findSkillTypesByChapterId(chapterId)).thenReturn(
                List.of(List.of())
        );

        final var actualSkillTypes = contentService.getAchievableSkillTypesByChapterIds(List.of(chapterId));

        assertThat(actualSkillTypes, contains(is(empty())));
    }
}