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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

@GraphQlApiTest
class MutationRemoveTagFromContentTest {

    @Container
    static final GitsPostgresSqlContainer postgres = GitsPostgresSqlContainer.getInstance();

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private TagRepository tagRepository;

    /**
     * Given a content with tags
     * When the removeTagFromContent mutation is called
     * Then the tag is removed from the content
     */
    @Test
    @Transactional
    void testRemoveTagFromContent(GraphQlTester graphQlTester) {
        ContentEntity contentEntity = contentRepository.save(TestData.dummyMediaContentEntityBuilder()
                .metadata(TestData.dummyContentMetadataEmbeddableBuilder()
                        .tags(Set.of(tagRepository.save(TagEntity.fromName("tag1")),
                                tagRepository.save(TagEntity.fromName("tag2"))))
                        .build())
                .build());

        String query = """
                mutation($contentId: UUID!, $tagName: String!) {
                    removeTagFromContent(contentId: $contentId, tagName: $tagName) {
                        id
                        metadata { tagNames }
                    }
                }
                """;

        graphQlTester.document(query)
                .variable("contentId", contentEntity.getId())
                .variable("tagName", "tag1")
                .execute()
                .path("removeTagFromContent.id").entity(UUID.class).isEqualTo(contentEntity.getId())
                .path("removeTagFromContent.metadata.tagNames")
                .entityList(String.class)
                .hasSize(1)
                .contains("tag2");

        ContentEntity updatedContentEntity = contentRepository.findById(contentEntity.getId()).orElseThrow();
        assertThat(updatedContentEntity.getMetadata().getTags(), hasSize(1));
        assertThat(updatedContentEntity.getTagNames(), containsInAnyOrder("tag2"));
    }

    /**
     * Given a content with tags
     * When the removeTagFromContent mutation is called with a tag that is not assigned to the content
     * Then the tag is not removed from the content
     */
    @Test
    @Transactional
    void testRemoveNonExistingTagFromContent(GraphQlTester graphQlTester) {
        ContentEntity contentEntity = contentRepository.save(TestData.dummyMediaContentEntityBuilder()
                .metadata(TestData.dummyContentMetadataEmbeddableBuilder()
                        .tags(Set.of(tagRepository.save(TagEntity.fromName("tag1")),
                                tagRepository.save(TagEntity.fromName("tag2"))))
                        .build())
                .build());

        String query = """
                mutation($contentId: UUID!, $tagName: String!) {
                    removeTagFromContent(contentId: $contentId, tagName: $tagName) {
                        id
                        metadata { tagNames }
                    }
                }
                """;

        graphQlTester.document(query)
                .variable("contentId", contentEntity.getId())
                .variable("tagName", "nonExistingTag")
                .execute()
                .path("removeTagFromContent.id").entity(UUID.class).isEqualTo(contentEntity.getId())
                .path("removeTagFromContent.metadata.tagNames")
                .entityList(String.class)
                .hasSize(2)
                .contains("tag1", "tag2");

        ContentEntity updatedContentEntity = contentRepository.findById(contentEntity.getId()).orElseThrow();
        assertThat(updatedContentEntity.getMetadata().getTags(), hasSize(2));
        assertThat(updatedContentEntity.getTagNames(), containsInAnyOrder("tag1", "tag2"));
    }
}
