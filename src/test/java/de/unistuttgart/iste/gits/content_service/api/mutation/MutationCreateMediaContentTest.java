package de.unistuttgart.iste.gits.content_service.api.mutation;

import de.unistuttgart.iste.gits.common.event.CrudOperation;
import de.unistuttgart.iste.gits.common.testutil.*;
import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser.UserRoleInCourse;
import de.unistuttgart.iste.gits.content_service.dapr.TopicPublisher;
import de.unistuttgart.iste.gits.content_service.persistence.entity.ContentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.entity.MediaContentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.repository.ContentRepository;
import de.unistuttgart.iste.gits.content_service.test_config.MockTopicPublisherConfiguration;
import de.unistuttgart.iste.gits.generated.dto.ContentType;
import de.unistuttgart.iste.gits.generated.dto.MediaContent;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ContextConfiguration;

import java.time.OffsetDateTime;
import java.util.UUID;

import static de.unistuttgart.iste.gits.common.testutil.TestUsers.userWithMembershipInCourseWithId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = MockTopicPublisherConfiguration.class)
@GraphQlApiTest
@TablesToDelete({"content_tags", "content"})
class MutationCreateMediaContentTest {

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private TopicPublisher topicPublisher;

    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser loggedInUser = userWithMembershipInCourseWithId(courseId, UserRoleInCourse.ADMINISTRATOR);

    @BeforeEach
    void beforeEach() {
        reset(topicPublisher);
    }

    /**
     * Given a valid CreateAssessmentInput
     * When the createAssessment mutation is called
     * Then a new assessment is created
     */
    @Test
    @Transactional
    @Commit
    void testCreateMediaContent(final GraphQlTester graphQlTester) {
        final UUID chapterId = UUID.randomUUID();
        final String query = """
                mutation($chapterId: UUID!, $courseId: UUID!) {
                    createMediaContent: _internal_createMediaContent(courseId: $courseId, input: {
                        metadata: {
                            chapterId: $chapterId
                            name: "name"
                            suggestedDate: "2021-01-01T00:00:00.000Z"
                            tagNames: ["tag1", "tag2"]
                            type: MEDIA
                            rewardPoints: 1
                        }
                    }) {
                        id
                        metadata {
                            name
                            tagNames
                            suggestedDate
                            type
                            chapterId
                            rewardPoints
                        }
                    }
                }
                """;

        final MediaContent createdMediaContent = graphQlTester.document(query)
                .variable("chapterId", chapterId)
                .variable("courseId", courseId)
                .execute()
                .path("createMediaContent").entity(MediaContent.class).get();

        // check that returned mediaContent is correct
        assertThat(createdMediaContent.getId(), is(notNullValue()));
        assertThat(createdMediaContent.getMetadata().getName(), is("name"));
        assertThat(createdMediaContent.getMetadata().getSuggestedDate(),
                is(OffsetDateTime.parse("2021-01-01T00:00:00.000Z")));
        assertThat(createdMediaContent.getMetadata().getTagNames(), containsInAnyOrder("tag1", "tag2"));
        assertThat(createdMediaContent.getMetadata().getType(), is(ContentType.MEDIA));
        assertThat(createdMediaContent.getMetadata().getChapterId(), is(chapterId));
        assertThat(createdMediaContent.getMetadata().getRewardPoints(), is(1));

        final ContentEntity contentEntity = contentRepository.findById(createdMediaContent.getId()).orElseThrow();
        assertThat(contentEntity, is(instanceOf(MediaContentEntity.class)));

        final MediaContentEntity mediaContentEntity = (MediaContentEntity) contentEntity;

        // check that mediaContent entity is correct
        assertThat(mediaContentEntity.getMetadata().getName(), is("name"));
        assertThat(mediaContentEntity.getMetadata().getSuggestedDate(),
                is(OffsetDateTime.parse("2021-01-01T00:00:00.000Z")));
        assertThat(mediaContentEntity.getMetadata().getTags(), containsInAnyOrder("tag1", "tag2"));
        assertThat(mediaContentEntity.getMetadata().getType(), is(ContentType.MEDIA));
        assertThat(mediaContentEntity.getMetadata().getChapterId(), is(chapterId));
        assertThat(mediaContentEntity.getMetadata().getRewardPoints(), is(1));

        verify(topicPublisher, atLeastOnce()).notifyChange(contentEntity, CrudOperation.CREATE);
    }

    /**
     * Given a CreateMediaContentInput with content type FLASHCARDS
     * When the createMediaContent mutation is called
     * Then a ValidationException is thrown
     */
    @Test
    void testCreateMediaContentWithFlashcardsType(final GraphQlTester graphQlTester) {
        final String query = """
                mutation($courseId: UUID!) {
                    createMediaContent: _internal_createMediaContent(courseId: $courseId, input: {
                        metadata: {
                            type: FLASHCARDS,
                            name: "name"
                            suggestedDate: "2021-01-01T00:00:00.000Z"
                            chapterId: "00000000-0000-0000-0000-000000000000"
                            rewardPoints: 1
                            tagNames: ["tag1", "tag2"]
                        }
                    }) { id }
                }
                """;

        graphQlTester.document(query)
                .variable("courseId", courseId)
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors, hasSize(1));
                    assertThat(errors.get(0).getMessage(), containsString("Media content must have type MEDIA"));
                    assertThat(errors.get(0).getExtensions().get("classification"), is("ValidationError"));
                });

        assertThat(contentRepository.findAll(), hasSize(0));
        verify(topicPublisher, never()).notifyChange(any(), any());
    }
}
