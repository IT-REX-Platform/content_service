package de.unistuttgart.iste.gits.content_service.api.mutation;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.TablesToDelete;
import de.unistuttgart.iste.gits.content_service.persistence.dao.StageEntity;
import de.unistuttgart.iste.gits.content_service.persistence.dao.SectionEntity;
import de.unistuttgart.iste.gits.content_service.persistence.repository.StageRepository;
import de.unistuttgart.iste.gits.content_service.persistence.repository.SectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.util.HashSet;
import java.util.UUID;

@GraphQlApiTest
@TablesToDelete({"stage_required_contents", "stage_optional_content", "stage" ,"section" , "content_tags", "user_progress_data", "content", "tag"})
class MutationDeleteStageTest {

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private StageRepository stageRepository;

    @Test
    void testStageDeletion(GraphQlTester tester){
        // fill database
        SectionEntity sectionEntity = SectionEntity.builder()
                .name("Test Section")
                .chapterId(UUID.randomUUID())
                .stages(new HashSet<>())
                .build();
        sectionEntity = sectionRepository.save(sectionEntity);

        StageEntity stageEntity = StageEntity.builder()
                .sectionId(sectionEntity.getId())
                .position(0)
                .optionalContent(new HashSet<>())
                .requiredContents(new HashSet<>())
                .build();
        stageEntity = stageRepository.save(stageEntity);

        String query = """
                mutation ($id: UUID!){
                deleteStage(id: $id)
                }
                """;
        tester.document(query)
                .variable("id", stageEntity.getId())
                .execute()
                .path("deleteStage")
                .entity(UUID.class)
                .isEqualTo(stageEntity.getId());
    }
}