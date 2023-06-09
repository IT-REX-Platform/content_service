package de.unistuttgart.iste.gits.content_service.api.mutation;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.TablesToDelete;
import de.unistuttgart.iste.gits.content_service.TestData;
import de.unistuttgart.iste.gits.content_service.persistence.dao.AssessmentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.dao.ContentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.repository.ContentRepository;
import de.unistuttgart.iste.gits.generated.dto.ContentType;
import de.unistuttgart.iste.gits.generated.dto.FlashcardSetAssessment;
import de.unistuttgart.iste.gits.generated.dto.SkillType;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.annotation.Commit;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@GraphQlApiTest
@TablesToDelete({"content_tags", "content", "tag"})
class MutationUpdateAssessmentTest {

    @Autowired
    private ContentRepository contentRepository;

    /**
     * Given a valid UpdateAssessmentInput
     * When the updateAssessment mutation is called
     * Then the assessment is updated
     */
    @Test
    @Transactional
    @Commit
    void testUpdateAssessment(GraphQlTester graphQlTester) {
        ContentEntity contentEntity = contentRepository.save(
                TestData.dummyAssessmentEntityBuilder().build());
        UUID newChapterId = UUID.randomUUID();

        String query = """
                mutation($assessmentId: UUID!, $chapterId: UUID!) {
                    updateAssessment(input: {
                        id: $assessmentId,
                        metadata: {
                            name: "newName",
                            suggestedDate: "2022-01-01T00:00:00.000Z",
                            chapterId: $chapterId,
                            rewardPoints: 3,
                            tagNames: ["newTag1", "newTag2"]
                        },
                        assessmentMetadata: {
                            skillPoints: 3,
                            skillType: UNDERSTAND
                            initialLearningInterval: 7
                        },
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
                            skillType
                            initialLearningInterval
                        }
                    }
                }
                """;

        FlashcardSetAssessment updatedAssessment = graphQlTester.document(query)
                .variable("assessmentId", contentEntity.getId())
                .variable("chapterId", newChapterId)
                .execute()
                .path("updateAssessment").entity(FlashcardSetAssessment.class).get();

        // check that returned assessment is correct
        assertThat(updatedAssessment.getId(), is(notNullValue()));
        assertThat(updatedAssessment.getMetadata().getName(), is("newName"));
        assertThat(updatedAssessment.getMetadata().getSuggestedDate(),
                is(OffsetDateTime.parse("2022-01-01T00:00:00.000Z")));
        assertThat(updatedAssessment.getMetadata().getTagNames(), containsInAnyOrder("newTag1", "newTag2"));
        assertThat(updatedAssessment.getMetadata().getType(), is(ContentType.FLASHCARDS));
        assertThat(updatedAssessment.getMetadata().getChapterId(), is(newChapterId));
        assertThat(updatedAssessment.getMetadata().getRewardPoints(), is(3));
        assertThat(updatedAssessment.getAssessmentMetadata().getSkillPoints(), is(3));
        assertThat(updatedAssessment.getAssessmentMetadata().getSkillType(), is(SkillType.UNDERSTAND));
        assertThat(updatedAssessment.getAssessmentMetadata().getInitialLearningInterval(), is(7));

        ContentEntity newContentEntity = contentRepository.findById(updatedAssessment.getId()).orElseThrow();
        assertThat(newContentEntity, is(instanceOf(AssessmentEntity.class)));

        AssessmentEntity assessmentEntity = (AssessmentEntity) newContentEntity;

        // check that assessment entity is correct
        assertThat(assessmentEntity.getMetadata().getName(), is("newName"));
        assertThat(assessmentEntity.getMetadata().getSuggestedDate(),
                is(OffsetDateTime.parse("2022-01-01T00:00:00.000Z")));
        assertThat(assessmentEntity.getMetadata().getRewardPoints(), is(3));
        assertThat(assessmentEntity.getTagNames(), containsInAnyOrder("newTag1", "newTag2"));
        assertThat(assessmentEntity.getMetadata().getType(), is(ContentType.FLASHCARDS));
        assertThat(assessmentEntity.getMetadata().getChapterId(), is(newChapterId));
        assertThat(assessmentEntity.getAssessmentMetadata().getSkillPoints(), is(3));
        assertThat(assessmentEntity.getAssessmentMetadata().getSkillType(), is(SkillType.UNDERSTAND));
        assertThat(assessmentEntity.getAssessmentMetadata().getInitialLearningInterval(), is(7));
    }
}
