package de.unistuttgart.iste.gits.content_service.api.mutation;

import de.unistuttgart.iste.gits.common.testutil.*;
import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.gits.content_service.TestData;
import de.unistuttgart.iste.gits.content_service.persistence.entity.ContentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.entity.MediaContentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.repository.ContentRepository;
import de.unistuttgart.iste.gits.generated.dto.ContentType;
import de.unistuttgart.iste.gits.generated.dto.MediaContent;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.annotation.Commit;

import java.time.OffsetDateTime;
import java.util.*;

import static de.unistuttgart.iste.gits.common.testutil.TestUsers.userWithMembershipInCourseWithId;
import static de.unistuttgart.iste.gits.common.user_handling.LoggedInUser.UserRoleInCourse.ADMINISTRATOR;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@GraphQlApiTest
@TablesToDelete({"content_tags", "content"})
class MutationUpdateMediaContentTest {

    @Autowired
    private ContentRepository contentRepository;

    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser loggedInUser = userWithMembershipInCourseWithId(courseId, ADMINISTRATOR);

    /**
     * Given a valid UpdateMediaContentInput
     * When the updateMediaContent mutation is called
     * Then the mediaContent is updated
     */
    @Test
    @Transactional
    @Commit
    void testUpdateMediaContent(final GraphQlTester graphQlTester) {
        final ContentEntity contentEntity = contentRepository.save(
                TestData.dummyMediaContentEntityBuilder(courseId).build());
        final UUID newChapterId = UUID.randomUUID();

        final String query = """
                mutation($contentId: UUID!, $chapterId: UUID!) {
                    mutateContent(contentId: $contentId){
                        updateMediaContent(input: {
                            metadata: {
                                name: "newName",
                                suggestedDate: "2022-01-01T00:00:00.000Z",
                                tagNames: ["newTag1", "newTag2"],
                                chapterId: $chapterId,
                                rewardPoints: 3
                            }
                        }) {
                            id
                            metadata {
                                name
                                suggestedDate
                                tagNames
                                type
                                chapterId
                                rewardPoints
                            }
                        }
                    }
                                
                }
                """;

        final MediaContent updatedMediaContent = graphQlTester.document(query)
                .variable("contentId", contentEntity.getId())
                .variable("chapterId", newChapterId)
                .execute()
                .path("mutateContent.updateMediaContent").entity(MediaContent.class).get();

        // check that returned mediaContent is correct
        assertThat(updatedMediaContent.getId(), is(notNullValue()));
        assertThat(updatedMediaContent.getMetadata().getName(), is("newName"));
        assertThat(updatedMediaContent.getMetadata().getSuggestedDate(),
                is(OffsetDateTime.parse("2022-01-01T00:00:00.000Z")));
        assertThat(updatedMediaContent.getMetadata().getTagNames(), containsInAnyOrder("newTag1", "newTag2"));
        assertThat(updatedMediaContent.getMetadata().getType(), is(ContentType.MEDIA));
        assertThat(updatedMediaContent.getMetadata().getChapterId(), is(newChapterId));
        assertThat(updatedMediaContent.getMetadata().getRewardPoints(), is(3));

        final ContentEntity newContentEntity = contentRepository.findById(updatedMediaContent.getId()).orElseThrow();
        assertThat(newContentEntity, is(instanceOf(MediaContentEntity.class)));

        final MediaContentEntity mediaContentEntity = (MediaContentEntity) newContentEntity;

        // check that mediaContent entity is correct
        assertThat(mediaContentEntity.getMetadata().getName(), is("newName"));
        assertThat(mediaContentEntity.getMetadata().getSuggestedDate(),
                is(OffsetDateTime.parse("2022-01-01T00:00:00.000Z")));
        assertThat(mediaContentEntity.getMetadata().getRewardPoints(), is(3));
        assertThat(mediaContentEntity.getMetadata().getTags(), containsInAnyOrder("newTag1", "newTag2"));
        assertThat(mediaContentEntity.getMetadata().getType(), is(ContentType.MEDIA));
        assertThat(mediaContentEntity.getMetadata().getChapterId(), is(newChapterId));
    }

    /**
     * Given a content with tags and an UpdateMediaContentInput with new tags, one of which is already present
     * When the updateMediaContent mutation is called
     * Then the tags are updated correctly
     */
    @Test
    @Transactional
    @Commit
    void testUpdateContentWithTagsCorrectly(final GraphQlTester graphQlTester) {
        final ContentEntity contentEntity = contentRepository.save(
                TestData.dummyMediaContentEntityBuilder(courseId)
                        .metadata(TestData.dummyContentMetadataEmbeddableBuilder(courseId)
                                .tags(new HashSet<>(Set.of("a", "b"))).build())
                        .build());
        final UUID newChapterId = UUID.randomUUID();

        final String query = """
                mutation($contentId: UUID!, $chapterId: UUID!) {
                    mutateContent(contentId: $contentId){
                        updateMediaContent(input: {
                            metadata: {
                                name: "newName",
                                suggestedDate: "2022-01-01T00:00:00.000Z",
                                tagNames: ["b", "c"],
                                chapterId: $chapterId,
                                rewardPoints: 3
                            }
                        }) {
                            id
                            metadata {
                                name
                                suggestedDate
                                tagNames
                                type
                                chapterId
                                rewardPoints
                            }
                        }
                    }
                                
                }
                """;

        final MediaContent updatedMediaContent = graphQlTester.document(query)
                .variable("contentId", contentEntity.getId())
                .variable("chapterId", newChapterId)
                .execute()
                .path("mutateContent.updateMediaContent").entity(MediaContent.class).get();

        assertThat(updatedMediaContent.getMetadata().getTagNames(), containsInAnyOrder("b", "c"));

        final ContentEntity newContentEntity = contentRepository.findById(updatedMediaContent.getId()).orElseThrow();
        assertThat(newContentEntity, is(instanceOf(MediaContentEntity.class)));

        final MediaContentEntity mediaContentEntity = (MediaContentEntity) newContentEntity;

        // check that tags are updated correctly
        assertThat(mediaContentEntity.getMetadata().getTags(), containsInAnyOrder("b", "c"));
    }
}
