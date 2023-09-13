package de.unistuttgart.iste.gits.content_service.api.mutation;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.TablesToDelete;
import de.unistuttgart.iste.gits.content_service.TestData;
import de.unistuttgart.iste.gits.content_service.persistence.dao.ContentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.dao.TagEntity;
import de.unistuttgart.iste.gits.content_service.persistence.repository.ContentRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.annotation.Commit;

import java.util.Set;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@GraphQlApiTest
@TablesToDelete({"content_tags", "content", "tag"})
class MutationAddTagToContentTest {

    @Autowired
    private ContentRepository contentRepository;

    /**
     * Given a content without tags
     * When the addTagToContent mutation is called
     * Then the tag is added to the content
     */
    @Test
    @Transactional
    @Commit
    void testAddTagToContent(GraphQlTester graphQlTester) {
        ContentEntity contentEntity = contentRepository.save(TestData.dummyMediaContentEntityBuilder().build());

        String query = """
                mutation($contentId: UUID!, $tagName: String!) {
                    mutateContent(contentId: $contentId){
                        addTagToContent(tagName: $tagName) {
                            id
                            metadata { tagNames }
                        }
                    }
                    
                }
                """;

        graphQlTester.document(query)
                .variable("contentId", contentEntity.getId())
                .variable("tagName", "tag")
                .execute()
                .path("mutateContent.addTagToContent.id").entity(UUID.class).isEqualTo(contentEntity.getId())
                .path("mutateContent.addTagToContent.metadata.tagNames").entityList(String.class).containsExactly("tag");

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
    @Commit
    void testAddTagToContentWithExistingTags(GraphQlTester graphQlTester) {
        ContentEntity contentEntity = contentRepository.save(TestData.dummyMediaContentEntityBuilder()
                .metadata(TestData.dummyContentMetadataEmbeddableBuilder()
                        .tags(Set.of(TagEntity.fromName("tag1")))
                        .build())
                .build());

        String query = """
                mutation($contentId: UUID!, $tagName: String!) {
                    mutateContent(contentId: $contentId){
                        addTagToContent(tagName: $tagName) {
                            id
                            metadata { tagNames }
                        }
                    }
                    
                }
                """;

        graphQlTester.document(query)
                .variable("contentId", contentEntity.getId())
                .variable("tagName", "tag2")
                .execute()
                .path("mutateContent.addTagToContent.id").entity(UUID.class).isEqualTo(contentEntity.getId())
                .path("mutateContent.addTagToContent.metadata.tagNames")
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
    @Commit
    void testAddDuplicateTagToContent(GraphQlTester graphQlTester) {
        ContentEntity contentEntity = contentRepository.save(TestData.dummyMediaContentEntityBuilder()
                .metadata(TestData.dummyContentMetadataEmbeddableBuilder()
                        .tags(Set.of(TagEntity.fromName("tag")))
                        .build())
                .build());

        String query = """
                mutation($contentId: UUID!, $tagName: String!) {
                    mutateContent(contentId: $contentId){
                        addTagToContent(tagName: $tagName) {
                            id
                            metadata { tagNames }
                        }
                    }
                    
                }
                """;

        graphQlTester.document(query)
                .variable("contentId", contentEntity.getId())
                .variable("tagName", "tag")
                .execute()
                .path("mutateContent.addTagToContent.metadata.tagNames")
                .entityList(String.class).hasSize(1).containsExactly("tag");

        ContentEntity updatedContentEntity = contentRepository.findById(contentEntity.getId()).orElseThrow();
        assertThat(updatedContentEntity.getMetadata().getTags(), hasSize(1));
        assertThat(updatedContentEntity.getTagNames(), contains("tag"));
    }
}
