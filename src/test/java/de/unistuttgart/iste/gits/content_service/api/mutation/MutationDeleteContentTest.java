package de.unistuttgart.iste.gits.content_service.api.mutation;

import de.unistuttgart.iste.gits.common.event.CrudOperation;
import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.TablesToDelete;
import de.unistuttgart.iste.gits.content_service.TestData;
import de.unistuttgart.iste.gits.content_service.dapr.TopicPublisher;
import de.unistuttgart.iste.gits.content_service.persistence.dao.ContentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.dao.TagEntity;
import de.unistuttgart.iste.gits.content_service.persistence.dao.UserProgressDataEntity;
import de.unistuttgart.iste.gits.content_service.persistence.repository.ContentRepository;
import de.unistuttgart.iste.gits.content_service.persistence.repository.TagRepository;
import de.unistuttgart.iste.gits.content_service.test_config.MockTopicPublisherConfiguration;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ContextConfiguration(classes = MockTopicPublisherConfiguration.class)
@GraphQlApiTest
@TablesToDelete({"content_tags", "user_progress_data", "content", "tag"})
class MutationDeleteContentTest {

    @Autowired
    private ContentRepository contentRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private TopicPublisher topicPublisher;

    /**
     * Given a UUID of an existing content
     * When the deleteContent mutation is executed
     * Then the content is deleted
     */
    @Test
    @Transactional
    @Commit
    void testDeleteExistingContent(GraphQlTester graphQlTester) {
        ContentEntity contentEntity = contentRepository.save(TestData.dummyAssessmentEntityBuilder()
                .metadata(TestData.dummyContentMetadataEmbeddableBuilder()
                        .tags(Set.of(
                                TagEntity.fromName("Tag"),
                                TagEntity.fromName("Tag2")))
                        .build())
                .userProgressData(List.of(
                        UserProgressDataEntity.builder()
                                .userId(UUID.randomUUID())
                                .learningInterval(2)
                                .build(),
                        UserProgressDataEntity.builder()
                                .userId(UUID.randomUUID())
                                .learningInterval(1)
                                .build()))
                .build());

        String query = """
                mutation($id: UUID!) {
                    deleteContent(id: $id)
                }
                """;

        graphQlTester.document(query)
                .variable("id", contentEntity.getId())
                .execute()
                .path("deleteContent").entity(UUID.class).isEqualTo(contentEntity.getId());

        assertThat(contentRepository.findById(contentEntity.getId()).isEmpty(), is(true));
        System.out.println(contentRepository.findAll());
        assertThat(contentRepository.count(), is(0L));
        // Tags are not deleted (yet)
        assertThat(tagRepository.count(), is(2L));

        verify(topicPublisher, times(1))
                .notifyChange(any(ContentEntity.class), eq(CrudOperation.DELETE));
    }

    /**
     * Given a UUID of a non-existing content
     * When the deleteContent mutation is executed
     * Then an error is returned
     */
    @Test
    void testDeleteNonExistingContent(GraphQlTester graphQlTester) {
        UUID id = UUID.randomUUID();
        String query = """
                mutation($id: UUID!) {
                    deleteContent(id: $id)
                }
                """;

        graphQlTester.document(query)
                .variable("id", id)
                .execute()
                .errors()
                .satisfy(responseErrors -> {
                    assertThat(responseErrors.size(), is(1));
                    assertThat(responseErrors.get(0).getExtensions().get("classification"),
                            is("DataFetchingException"));
                    assertThat(responseErrors.get(0).getMessage(),
                            containsString("Content with id " + id + " not found"));
                });

    }
}
