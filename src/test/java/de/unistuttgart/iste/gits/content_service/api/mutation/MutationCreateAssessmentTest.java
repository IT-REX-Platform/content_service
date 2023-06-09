package de.unistuttgart.iste.gits.content_service.api.mutation;


import de.unistuttgart.iste.gits.common.event.CrudOperation;
import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.TablesToDelete;
import de.unistuttgart.iste.gits.content_service.dapr.TopicPublisher;
import de.unistuttgart.iste.gits.content_service.persistence.dao.AssessmentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.dao.ContentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.repository.ContentRepository;
import de.unistuttgart.iste.gits.content_service.test_config.MockTopicPublisherConfiguration;
import de.unistuttgart.iste.gits.generated.dto.ContentType;
import de.unistuttgart.iste.gits.generated.dto.FlashcardSetAssessment;
import de.unistuttgart.iste.gits.generated.dto.SkillType;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = MockTopicPublisherConfiguration.class)
@GraphQlApiTest
@TablesToDelete({"content_tags", "content", "tag"})
class MutationCreateAssessmentTest {

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private TopicPublisher topicPublisher;

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
    void testCreateAssessment(GraphQlTester graphQlTester) {
        UUID chapterId = UUID.randomUUID();
        String query = """
                mutation($chapterId: UUID!) {
                    createAssessment(input: {
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
                            skillType: REMEMBER
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
                            skillType
                            initialLearningInterval
                        }
                    }
                }
                """;

        FlashcardSetAssessment createdAssessment = graphQlTester.document(query)
                .variable("chapterId", chapterId)
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
        assertThat(createdAssessment.getAssessmentMetadata().getSkillType(), is(SkillType.REMEMBER));
        assertThat(createdAssessment.getAssessmentMetadata().getInitialLearningInterval(), is(2));

        ContentEntity contentEntity = contentRepository.findById(createdAssessment.getId()).orElseThrow();
        assertThat(contentEntity, is(instanceOf(AssessmentEntity.class)));

        AssessmentEntity assessmentEntity = (AssessmentEntity) contentEntity;

        // check that assessment entity is correct
        assertThat(assessmentEntity.getMetadata().getName(), is("name"));
        assertThat(assessmentEntity.getMetadata().getSuggestedDate(),
                is(LocalDate.of(2021, 1, 1).atStartOfDay().atOffset(ZoneOffset.UTC)));
        assertThat(assessmentEntity.getTagNames(), containsInAnyOrder("tag1", "tag2"));
        assertThat(assessmentEntity.getMetadata().getType(), is(ContentType.FLASHCARDS));
        assertThat(assessmentEntity.getMetadata().getChapterId(), is(chapterId));
        assertThat(assessmentEntity.getMetadata().getRewardPoints(), is(1));
        assertThat(assessmentEntity.getAssessmentMetadata().getSkillPoints(), is(1));
        assertThat(assessmentEntity.getAssessmentMetadata().getSkillType(), is(SkillType.REMEMBER));
        assertThat(assessmentEntity.getAssessmentMetadata().getInitialLearningInterval(), is(2));

        verify(topicPublisher, times(1))
                .notifyChange(assessmentEntity, CrudOperation.CREATE);

    }

    /**
     * Given a CreateAssessmentInput with content type MEDIA
     * When the createAssessment mutation is called
     * Then a ValidationException is thrown
     */
    @Test
    void testCreateAssessmentWithMediaContentType(GraphQlTester graphQlTester) {
        String query = """
                mutation {
                    createAssessment(input: {
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
                            skillType: REMEMBER
                            initialLearningInterval: 1
                        }
                    }) { id }
                }
                """;

        graphQlTester.document(query)
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors, hasSize(1));
                    assertThat(errors.get(0).getMessage(), containsString("MEDIA is not a valid content type for an assessment"));
                    assertThat(errors.get(0).getExtensions().get("classification"), is("ValidationError"));
                });

        assertThat(contentRepository.count(), is(0L));

        verify(topicPublisher, never()).notifyChange(any(), any());
    }
}
