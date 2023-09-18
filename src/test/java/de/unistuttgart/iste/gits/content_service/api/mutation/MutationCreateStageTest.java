package de.unistuttgart.iste.gits.content_service.api.mutation;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.TablesToDelete;
import de.unistuttgart.iste.gits.content_service.persistence.entity.SectionEntity;
import de.unistuttgart.iste.gits.content_service.persistence.repository.SectionRepository;
import de.unistuttgart.iste.gits.generated.dto.CreateStageInput;
import de.unistuttgart.iste.gits.generated.dto.Stage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@GraphQlApiTest
@TablesToDelete({"stage_required_contents", "stage_optional_contents", "stage", "section", "content_tags", "user_progress_data", "content"})
class MutationCreateStageTest {

    @Autowired
    private SectionRepository sectionRepository;


    @Test
    void testStageCreation(GraphQlTester tester) {
        List<UUID> contentIds = new ArrayList<>();

        SectionEntity sectionEntity = SectionEntity.builder()
                .name("Test Section")
                .chapterId(UUID.randomUUID())
                .stages(new HashSet<>())
                .build();
        sectionEntity = sectionRepository.save(sectionEntity);

        CreateStageInput stageInput = CreateStageInput.builder()
                .setRequiredContents(contentIds)
                .setOptionalContents(contentIds)
                .build();

        String query = """
                mutation($id: UUID!, $input: CreateStageInput){
                    mutateSection(sectionId: $id){
                        createStage(input: $input) {
                            id
                            position
                            requiredContents {
                                id        
                            }     
                            optionalContents {
                                id                    
                            }           
                    }
                }
                                
                }
                """;
        tester.document(query)
                .variable("id", sectionEntity.getId())
                .variable("input", stageInput).execute()
                .path("mutateSection.createStage")
                .entity(Stage.class)
                .satisfies( stage -> {
            assertEquals(0, stage.getPosition());
            assertTrue(stage.getRequiredContents().isEmpty());
            assertTrue(stage.getOptionalContents().isEmpty());
            assertNotNull(stage.getId());
        });
    }
}
