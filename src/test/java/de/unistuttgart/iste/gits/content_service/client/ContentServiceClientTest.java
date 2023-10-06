package de.unistuttgart.iste.gits.content_service.client;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.content_service.TestData;
import de.unistuttgart.iste.gits.content_service.persistence.entity.AssessmentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.entity.ContentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.repository.ContentRepository;
import de.unistuttgart.iste.gits.generated.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

/**
 * This class is used to test the ContentServiceClient.
 */
@GraphQlApiTest
class ContentServiceClientTest {

    private GraphQlClient graphQlClient;

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private ContentRepository contentRepository;

    @BeforeEach
    void setUp() {
        final WebTestClient webTestClient = MockMvcWebTestClient.bindToApplicationContext(applicationContext)
                .configureClient().baseUrl("/graphql").build();

        graphQlClient = GraphQlClient.builder(new WebTestClientTransport(webTestClient)).build();
    }

    @Test
    void testQueryContentsOfChapter() throws Exception {
        final ContentServiceClient contentServiceClient = new ContentServiceClient(graphQlClient);
        final UUID courseId = UUID.randomUUID();
        final UUID chapterId = UUID.randomUUID();
        final UUID userId = UUID.randomUUID();

        contentRepository.save(createMediaContentForChapter(courseId, chapterId));
        contentRepository.save(createAssessmentForChapter(courseId, chapterId, ContentType.FLASHCARDS));
        contentRepository.save(createAssessmentForChapter(courseId, chapterId, ContentType.QUIZ));

        final List<Content> actualContents = contentServiceClient.queryContentsOfChapter(userId, chapterId);

        assertThat(actualContents, hasSize(3));

        // we just check the types here exemplary, other fields are tested in the API tests
        final var types = actualContents.stream().map(Content::getMetadata).map(ContentMetadata::getType).toList();

        assertThat(types, containsInAnyOrder(ContentType.MEDIA, ContentType.FLASHCARDS, ContentType.QUIZ));
    }

    @Test
    void testQueryContentsOfCourse() throws Exception {
        final ContentServiceClient contentServiceClient = new ContentServiceClient(graphQlClient);
        final UUID courseId = UUID.randomUUID();
        final UUID chapterId = UUID.randomUUID();
        final UUID userId = UUID.randomUUID();

        contentRepository.save(createMediaContentForChapter(courseId, chapterId));
        contentRepository.save(createAssessmentForChapter(courseId, chapterId, ContentType.FLASHCARDS));
        contentRepository.save(createAssessmentForChapter(courseId, chapterId, ContentType.QUIZ));

        final List<Content> actualContents = contentServiceClient.queryContentsOfCourse(userId, courseId);

        assertThat(actualContents, hasSize(3));

        // we just check the types here exemplary, other fields are tested in the API tests
        final var types = actualContents.stream().map(Content::getMetadata).map(ContentMetadata::getType).toList();

        assertThat(types, containsInAnyOrder(ContentType.MEDIA, ContentType.FLASHCARDS, ContentType.QUIZ));
    }

    private ContentEntity createMediaContentForChapter(final UUID courseId, final UUID chapterId) {
        return TestData.dummyMediaContentEntityBuilder(courseId)
                .metadata(TestData.dummyContentMetadataEmbeddableBuilder(courseId)
                        .chapterId(chapterId)
                        .build())
                .build();
    }

    private AssessmentEntity createAssessmentForChapter(final UUID courseId, final UUID chapterId, final ContentType type) {
        return TestData.dummyAssessmentEntityBuilder(courseId)
                .metadata(TestData.dummyContentMetadataEmbeddableBuilder(courseId)
                        .type(type)
                        .chapterId(chapterId)
                        .build())
                .build();
    }
}
