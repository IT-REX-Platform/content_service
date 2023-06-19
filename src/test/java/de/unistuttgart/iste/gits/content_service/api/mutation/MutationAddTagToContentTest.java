package de.unistuttgart.iste.gits.content_service.api.mutation;

import de.unistuttgart.iste.gits.common.testutil.GitsPostgresSqlContainer;
import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.content_service.TestData;
import de.unistuttgart.iste.gits.content_service.persistence.dao.ContentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.dao.TagEntity;
import de.unistuttgart.iste.gits.content_service.persistence.repository.ContentRepository;
import de.unistuttgart.iste.gits.content_service.persistence.repository.TagRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.testcontainers.junit.jupiter.Container;

import java.util.Set;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@GraphQlApiTest
class MutationAddTagToContentTest {

    @Container
    static final GitsPostgresSqlContainer postgres = GitsPostgresSqlContainer.getInstance();

    @Autowired
    private ContentRepository contentRepository;
    @Autowired
    private TagRepository tagRepository;

    /**
     * Given a content without tags
     * When the addTagToContent mutation is called
     * Then the tag is added to the content
     */
    @Test
    @Transactional
    void testAddTagToContent(GraphQlTester graphQlTester) {
        ContentEntity contentEntity = contentRepository.save(TestData.dummyMediaContentEntityBuilder().build());

        String query = """
                mutation($contentId: UUID!, $tagName: String!) {
                    addTagToContent(contentId: $contentId, tagName: $tagName) {
                        id
                        metadata { tagNames }
                    }
                }
                """;

        graphQlTester.document(query)
                .variable("contentId", contentEntity.getId())
                .variable("tagName", "tag")
                .execute()
                .path("addTagToContent.id").entity(UUID.class).isEqualTo(contentEntity.getId())
                .path("addTagToContent.metadata.tagNames").entityList(String.class).containsExactly("tag");

        ContentEntity updatedContentEntity = contentRepository.findById(contentEntity.getId()).orElseThrow();
        assertThat(updatedContentEntity.getMetadata().getTags(), hasSize(1));
        assertThat(updatedContentEntity.getMetadata().getTags().iterator().next().getName(), is("tag"));
    }

    /**
     * Given a content with a tag
     * When the addTagToContent mutation is called with a different tag
     * Then the tag is added to the content
     */
    @Test
    @Transactional
    void testAddTagToContentWithExistingTags(GraphQlTester graphQlTester) {
        ContentEntity contentEntity = contentRepository.save(TestData.dummyMediaContentEntityBuilder()
                .metadata(TestData.dummyContentMetadataEmbeddableBuilder()
                        .tags(Set.of(tagRepository.save(TagEntity.fromName("tag1"))))
                        .build())
                .build());

        String query = """
                mutation($contentId: UUID!, $tagName: String!) {
                    addTagToContent(contentId: $contentId, tagName: $tagName) {
                        id
                        metadata { tagNames }
                    }
                }
                """;

        graphQlTester.document(query)
                .variable("contentId", contentEntity.getId())
                .variable("tagName", "tag2")
                .execute()
                .path("addTagToContent.id").entity(UUID.class).isEqualTo(contentEntity.getId())
                .path("addTagToContent.metadata.tagNames")
                .entityList(String.class)
                .hasSize(2)
                .contains("tag1", "tag2");

        ContentEntity updatedContentEntity = contentRepository.findById(contentEntity.getId()).orElseThrow();
        assertThat(updatedContentEntity.getMetadata().getTags(), hasSize(2));
        assertThat(updatedContentEntity.getTagNames(), containsInAnyOrder("tag1", "tag2"));
    }

    /**
     * Given a content with a tag
     * When the addTagToContent mutation is called with the same tag
     * Then the tag is not added to the content
     */
    @Test
    @Transactional
    void testAddDuplicateTagToContent(GraphQlTester graphQlTester) {
        ContentEntity contentEntity = contentRepository.save(TestData.dummyMediaContentEntityBuilder()
                .metadata(TestData.dummyContentMetadataEmbeddableBuilder()
                        .tags(Set.of(tagRepository.save(TagEntity.fromName("tag"))))
                        .build())
                .build());

        String query = """
                mutation($contentId: UUID!, $tagName: String!) {
                    addTagToContent(contentId: $contentId, tagName: $tagName) {
                        id
                        metadata { tagNames }
                    }
                }
                """;

        graphQlTester.document(query)
                .variable("contentId", contentEntity.getId())
                .variable("tagName", "tag")
                .execute()
                .path("addTagToContent.metadata.tagNames")
                .entityList(String.class).hasSize(1).containsExactly("tag");

        ContentEntity updatedContentEntity = contentRepository.findById(contentEntity.getId()).orElseThrow();
        assertThat(updatedContentEntity.getMetadata().getTags(), hasSize(1));
        assertThat(updatedContentEntity.getTagNames(), contains("tag"));
    }
}
