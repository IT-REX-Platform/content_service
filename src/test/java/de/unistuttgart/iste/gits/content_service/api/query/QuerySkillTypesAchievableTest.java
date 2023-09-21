package de.unistuttgart.iste.gits.content_service.api.query;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.content_service.TestData;
import de.unistuttgart.iste.gits.content_service.persistence.entity.AssessmentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.repository.ContentRepository;
import de.unistuttgart.iste.gits.generated.dto.SkillType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.util.UUID;

@GraphQlApiTest
class QuerySkillTypesAchievableTest {

    @Autowired
    private ContentRepository contentRepository;

    @Test
    void testQuerySkillTypesByChapterId(GraphQlTester graphQlTester) {
        UUID chapterId = UUID.randomUUID();

        AssessmentEntity assessmentEntity1 = TestData.assessmentEntityWithSkillType(chapterId, SkillType.UNDERSTAND);
        AssessmentEntity assessmentEntity2 = TestData.assessmentEntityWithSkillType(chapterId, SkillType.APPLY);

        assessmentEntity1 = contentRepository.save(assessmentEntity1);
        assessmentEntity2 = contentRepository.save(assessmentEntity2);

        String query = """
                query ($chapterId: UUID!) {
                    _internal_noauth_achievableSkillTypesByChapterIds(chapterIds: [$chapterId])
                }
                """;

        graphQlTester.document(query)
                .variable("chapterId", chapterId)
                .execute()
                .path("_internal_achievableSkillTypesByChapterIds[0][*]")
                .entityList(SkillType.class)
                .contains(assessmentEntity1.getAssessmentMetadata().getSkillTypes().get(0),
                        assessmentEntity2.getAssessmentMetadata().getSkillTypes().get(0));

    }

}
