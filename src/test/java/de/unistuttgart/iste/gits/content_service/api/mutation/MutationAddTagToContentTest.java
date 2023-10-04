package de.unistuttgart.iste.gits.content_service.api.mutation;

import de.unistuttgart.iste.gits.common.testutil.*;
import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser.UserRoleInCourse;
import de.unistuttgart.iste.gits.content_service.TestData;
import de.unistuttgart.iste.gits.content_service.persistence.entity.ContentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.repository.ContentRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.annotation.Commit;

import java.util.Set;
import java.util.UUID;

import static de.unistuttgart.iste.gits.common.testutil.TestUsers.userWithMembershipInCourseWithId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@GraphQlApiTest
@TablesToDelete({"content_tags", "content"})
class MutationAddTagToContentTest {

    @Autowired
    private ContentRepository contentRepository;

    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser loggedInUser = userWithMembershipInCourseWithId(courseId, UserRoleInCourse.ADMINISTRATOR);

    /**
     * Given a content without tags
     * When the addTagToContent mutation is called
     * Then the tag is added to the content
     */
    @Test
    @Transactional
    @Commit
    void testAddTagToContent(final GraphQlTester graphQlTester) {
        final ContentEntity contentEntity = contentRepository.save(TestData.dummyMediaContentEntityBuilder(courseId).build());

        executeAddTagToContentMutation(graphQlTester, contentEntity.getId(), "tag")
                .path("mutateContent.addTagToContent.id").entity(UUID.class).isEqualTo(contentEntity.getId())
                .path("mutateContent.addTagToContent.metadata.tagNames").entityList(String.class).containsExactly("tag");

        final ContentEntity updatedContentEntity = contentRepository.findById(contentEntity.getId()).orElseThrow();
        assertThat(updatedContentEntity.getMetadata().getTags(), hasSize(1));
        assertThat(updatedContentEntity.getMetadata().getTags().iterator().next(), is("tag"));
    }

    /**
     * Given a content with a tag
     * When the addTagToContent mutation is called with a different tag
     * Then the tag is added to the content
     */
    @Test
    @Transactional
    @Commit
    void testAddTagToContentWithExistingTags(final GraphQlTester graphQlTester) {
        final ContentEntity contentEntity = contentRepository.save(TestData.dummyMediaContentEntityBuilder(courseId)
                .metadata(TestData.dummyContentMetadataEmbeddableBuilder(courseId)
                        .tags(Set.of("tag1"))
                        .build())
                .build());

        executeAddTagToContentMutation(graphQlTester, contentEntity.getId(), "tag2")
                .path("mutateContent.addTagToContent.id").entity(UUID.class).isEqualTo(contentEntity.getId())
                .path("mutateContent.addTagToContent.metadata.tagNames")
                .entityList(String.class)
                .hasSize(2)
                .contains("tag1", "tag2");

        final ContentEntity updatedContentEntity = contentRepository.findById(contentEntity.getId()).orElseThrow();
        assertThat(updatedContentEntity.getMetadata().getTags(), hasSize(2));
        assertThat(updatedContentEntity.getMetadata().getTags(), containsInAnyOrder("tag1", "tag2"));
    }

    /**
     * Given a content with a tag
     * When the addTagToContent mutation is called with the same tag
     * Then the tag is not added to the content
     */
    @Test
    @Transactional
    @Commit
    void testAddDuplicateTagToContent(final GraphQlTester graphQlTester) {
        final ContentEntity contentEntity = contentRepository.save(TestData.dummyMediaContentEntityBuilder(courseId)
                .metadata(TestData.dummyContentMetadataEmbeddableBuilder(courseId)
                        .tags(Set.of("tag"))
                        .build())
                .build());

        executeAddTagToContentMutation(graphQlTester, contentEntity.getId(), "tag")
                .path("mutateContent.addTagToContent.metadata.tagNames")
                .entityList(String.class)
                .hasSize(1)
                .containsExactly("tag");

        final ContentEntity updatedContentEntity = contentRepository.findById(contentEntity.getId()).orElseThrow();
        assertThat(updatedContentEntity.getMetadata().getTags(), hasSize(1));
        assertThat(updatedContentEntity.getMetadata().getTags(), contains("tag"));
    }

    private GraphQlTester.Response executeAddTagToContentMutation(final GraphQlTester graphQlTester, final UUID contentId, final String tagName) {
        final String query = """
                mutation($contentId: UUID!, $tagName: String!) {
                    mutateContent(contentId: $contentId){
                        addTagToContent(tagName: $tagName) {
                            id
                            metadata { tagNames }
                        }
                    }
                    
                }
                """;

        return graphQlTester.document(query)
                .variable("contentId", contentId)
                .variable("tagName", tagName)
                .execute();
    }
}
