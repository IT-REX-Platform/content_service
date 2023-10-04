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
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;

@GraphQlApiTest
@TablesToDelete({"content_tags", "content"})
class MutationRemoveTagFromContentTest {

    @Autowired
    private ContentRepository contentRepository;

    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser loggedInUser = userWithMembershipInCourseWithId(courseId, UserRoleInCourse.ADMINISTRATOR);

    /**
     * Given a content with tags
     * When the removeTagFromContent mutation is called
     * Then the tag is removed from the content
     */
    @Test
    @Transactional
    @Commit
    void testRemoveTagFromContent(final GraphQlTester graphQlTester) {
        final ContentEntity contentEntity = contentRepository.save(TestData.dummyMediaContentEntityBuilder(courseId)
                .metadata(TestData.dummyContentMetadataEmbeddableBuilder(courseId)
                        .tags(Set.of("tag1", "tag2"))
                        .build())
                .build());

        final String query = """
                mutation($contentId: UUID!, $tagName: String!) {
                    mutateContent(contentId: $contentId){
                        removeTagFromContent(tagName: $tagName) {
                            id
                            metadata { tagNames }
                        }
                    }
                    
                }
                """;

        graphQlTester.document(query)
                .variable("contentId", contentEntity.getId())
                .variable("tagName", "tag1")
                .execute()
                .path("mutateContent.removeTagFromContent.id").entity(UUID.class).isEqualTo(contentEntity.getId())
                .path("mutateContent.removeTagFromContent.metadata.tagNames")
                .entityList(String.class)
                .hasSize(1)
                .contains("tag2");

        final ContentEntity updatedContentEntity = contentRepository.findById(contentEntity.getId()).orElseThrow();
        assertThat(updatedContentEntity.getMetadata().getTags(), hasSize(1));
        assertThat(updatedContentEntity.getMetadata().getTags(), containsInAnyOrder("tag2"));

    }

    /**
     * Given a content with tags
     * When the removeTagFromContent mutation is called with a tag that is not assigned to the content
     * Then the tag is not removed from the content
     */
    @Test
    @Transactional
    @Commit
    void testRemoveNonExistingTagFromContent(final GraphQlTester graphQlTester) {
        final ContentEntity contentEntity = contentRepository.save(TestData.dummyMediaContentEntityBuilder(courseId)
                .metadata(TestData.dummyContentMetadataEmbeddableBuilder(courseId)
                        .tags(Set.of("tag1", "tag2"))
                        .build())
                .build());

        final String query = """
                mutation($contentId: UUID!, $tagName: String!) {
                    mutateContent(contentId: $contentId){
                        removeTagFromContent(tagName: $tagName) {
                            id
                            metadata { tagNames }
                        }
                    }
                    
                }
                """;

        graphQlTester.document(query)
                .variable("contentId", contentEntity.getId())
                .variable("tagName", "nonExistingTag")
                .execute()
                .path("mutateContent.removeTagFromContent.id").entity(UUID.class).isEqualTo(contentEntity.getId())
                .path("mutateContent.removeTagFromContent.metadata.tagNames")
                .entityList(String.class)
                .hasSize(2)
                .contains("tag1", "tag2");

        final ContentEntity updatedContentEntity = contentRepository.findById(contentEntity.getId()).orElseThrow();
        assertThat(updatedContentEntity.getMetadata().getTags(), hasSize(2));
        assertThat(updatedContentEntity.getMetadata().getTags(), containsInAnyOrder("tag1", "tag2"));
    }


}
