package de.unistuttgart.iste.gits.content_service.api.mutation;


import de.unistuttgart.iste.gits.common.testutil.*;
import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.gits.content_service.persistence.entity.AssessmentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.entity.ContentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.repository.ContentRepository;
import de.unistuttgart.iste.gits.generated.dto.*;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.annotation.Commit;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static de.unistuttgart.iste.gits.common.testutil.TestUsers.userWithMembershipInCourseWithId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@GraphQlApiTest
@TablesToDelete({"content_tags", "content"})
class MutationCreateAssessmentTest {

    @Autowired
    private ContentRepository contentRepository;

    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser loggedInUser = userWithMembershipInCourseWithId(courseId, LoggedInUser.UserRoleInCourse.ADMINISTRATOR);

    /**
     * Given a valid CreateAssessmentInput
     * When the createAssessment mutation is called
     * Then a new assessment is created
     */
    @Test
    @Transactional
    @Commit
    void testCreateAssessment(final GraphQlTester graphQlTester) {
        final UUID chapterId = UUID.randomUUID();
        final String query = """
                mutation($chapterId: UUID!, $courseId: UUID!) {
                    createAssessment: _internal_createAssessment(courseId: $courseId, input: {
                        metadata: {
                            chapterId: $chapterId
                            name: "name"
                            suggestedDate: "2021-01-01T00:00:00.000Z"
                            rewardPoints: 1
                            tagNames: ["tag1", "tag2"]
                            type: FLASHCARDS
                        }
                        assessmentMetadata: {
                            skillPoints: 1
                            skillTypes: [REMEMBER]
                            initialLearningInterval: 2
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
                        assessmentMetadata {
                            skillPoints
                            skillTypes
                            initialLearningInterval
                        }
                    }
                }
                """;

        final FlashcardSetAssessment createdAssessment = graphQlTester.document(query)
                .variable("chapterId", chapterId)
                .variable("courseId", courseId)
                .execute()
                .path("createAssessment").entity(FlashcardSetAssessment.class).get();

        // check that returned assessment is correct
        assertThat(createdAssessment.getId(), is(notNullValue()));
        assertThat(createdAssessment.getMetadata().getName(), is("name"));
        assertThat(createdAssessment.getMetadata().getSuggestedDate(),
                is(LocalDate.of(2021, 1, 1).atStartOfDay().atOffset(ZoneOffset.UTC)));
        assertThat(createdAssessment.getMetadata().getTagNames(), containsInAnyOrder("tag1", "tag2"));
        assertThat(createdAssessment.getMetadata().getType(), is(ContentType.FLASHCARDS));
        assertThat(createdAssessment.getMetadata().getChapterId(), is(chapterId));
        assertThat(createdAssessment.getMetadata().getRewardPoints(), is(1));
        assertThat(createdAssessment.getAssessmentMetadata().getSkillPoints(), is(1));
        assertThat(createdAssessment.getAssessmentMetadata().getSkillTypes(), is(List.of(SkillType.REMEMBER)));
        assertThat(createdAssessment.getAssessmentMetadata().getInitialLearningInterval(), is(2));

        final ContentEntity contentEntity = contentRepository.findById(createdAssessment.getId()).orElseThrow();
        assertThat(contentEntity, is(instanceOf(AssessmentEntity.class)));

        final AssessmentEntity assessmentEntity = (AssessmentEntity) contentEntity;

        // check that assessment entity is correct
        assertThat(assessmentEntity.getMetadata().getName(), is("name"));
        assertThat(assessmentEntity.getMetadata().getSuggestedDate(),
                is(LocalDate.of(2021, 1, 1).atStartOfDay().atOffset(ZoneOffset.UTC)));
        assertThat(assessmentEntity.getMetadata().getTags(), containsInAnyOrder("tag1", "tag2"));
        assertThat(assessmentEntity.getMetadata().getType(), is(ContentType.FLASHCARDS));
        assertThat(assessmentEntity.getMetadata().getChapterId(), is(chapterId));
        assertThat(assessmentEntity.getMetadata().getRewardPoints(), is(1));
        assertThat(assessmentEntity.getAssessmentMetadata().getSkillPoints(), is(1));
        assertThat(assessmentEntity.getAssessmentMetadata().getSkillTypes(), is(List.of(SkillType.REMEMBER)));
        assertThat(assessmentEntity.getAssessmentMetadata().getInitialLearningInterval(), is(2));

    }

    /**
     * Given a CreateAssessmentInput with content type MEDIA
     * When the createAssessment mutation is called
     * Then a ValidationException is thrown
     */
    @Test
    void testCreateAssessmentWithMediaContentType(final GraphQlTester graphQlTester) {
        final String query = """
                mutation($courseId: UUID!) {
                    createAssessment: _internal_createAssessment(courseId: $courseId, input: {
                        metadata: {
                            type: MEDIA
                            name: "name"
                            suggestedDate: "2021-01-01T00:00:00.000Z"
                            chapterId: "00000000-0000-0000-0000-000000000000"
                            rewardPoints: 1
                            tagNames: ["tag1", "tag2"]
                        }
                        assessmentMetadata: {
                            skillPoints: 1
                            skillTypes: [REMEMBER]
                            initialLearningInterval: 1
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
                    assertThat(errors.get(0).getMessage(), containsString("MEDIA is not a valid content type for an assessment"));
                    assertThat(errors.get(0).getExtensions().get("classification"), is("ValidationError"));
                });

        assertThat(contentRepository.count(), is(0L));
    }
}
