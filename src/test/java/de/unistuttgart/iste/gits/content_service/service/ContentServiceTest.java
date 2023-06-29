package de.unistuttgart.iste.gits.content_service.service;

import de.unistuttgart.iste.gits.common.event.ChapterChangeEvent;
import de.unistuttgart.iste.gits.common.event.CrudOperation;
import de.unistuttgart.iste.gits.common.event.ResourceUpdateEvent;
import de.unistuttgart.iste.gits.content_service.dapr.TopicPublisher;
import de.unistuttgart.iste.gits.content_service.persistence.dao.ContentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.dao.ContentMetadataEmbeddable;
import de.unistuttgart.iste.gits.content_service.persistence.mapper.ContentMapper;
import de.unistuttgart.iste.gits.content_service.persistence.repository.ContentRepository;
import de.unistuttgart.iste.gits.content_service.persistence.repository.TagRepository;
import de.unistuttgart.iste.gits.content_service.test_config.MockTopicPublisherConfiguration;
import de.unistuttgart.iste.gits.content_service.validation.ContentValidator;
import de.unistuttgart.iste.gits.generated.dto.ContentType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.test.context.ContextConfiguration;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = MockTopicPublisherConfiguration.class)
class ContentServiceTest {

    private final ContentRepository contentRepository = Mockito.mock(ContentRepository.class);
    private final TagRepository tagRepository = Mockito.mock(TagRepository.class);
    private final ContentMapper contentMapper = new ContentMapper(new ModelMapper());
    private final ContentValidator contentValidator = Mockito.spy(ContentValidator.class);
    private final TagService tagService = Mockito.mock(TagService.class);
    private final TopicPublisher mockPublisher = Mockito.mock(TopicPublisher.class);

    private final ContentService contentService = new ContentService(contentRepository, tagRepository, contentMapper, contentValidator, tagService, mockPublisher);

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

        contentService.forwardResourceUpdates(dto);


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
        assertThrows(NullPointerException.class, () -> contentService.forwardResourceUpdates(noEntityDto));
        assertThrows(NullPointerException.class, () -> contentService.forwardResourceUpdates(nullListDto));
        assertThrows(NullPointerException.class, () -> contentService.forwardResourceUpdates(emptyListDto));
        assertThrows(NullPointerException.class, () -> contentService.forwardResourceUpdates(noOperationDto));
    }

    @Test
    void cascadeContentDeletion() {

        ChapterChangeEvent dto = ChapterChangeEvent.builder()
                .chapterIds(List.of(UUID.randomUUID(), UUID.randomUUID()))
                .operation(CrudOperation.DELETE)
                .build();

        ContentEntity testEntity = ContentEntity.builder()
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
        ContentEntity testEntity2 = ContentEntity.builder()
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
        contentService.cascadeContentDeletion(dto);

        verify(contentRepository, times(2)).delete(any(ContentEntity.class));
        verify(mockPublisher, times(1)).informContentDependantServices(List.of(testEntity.getId(), testEntity2.getId()), CrudOperation.DELETE);
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
        verify(mockPublisher, times(0)).informContentDependantServices(any(), any());


        //execute method under test
        assertThrows(NullPointerException.class, () -> contentService.cascadeContentDeletion(emptyListDto));
        assertThrows(NullPointerException.class, () -> contentService.cascadeContentDeletion(nullListDto));
        assertThrows(NullPointerException.class, () -> contentService.cascadeContentDeletion(noOperationDto));
    }
}