package de.unistuttgart.iste.gits.content_service.api.mutation;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.TablesToDelete;
import de.unistuttgart.iste.gits.content_service.persistence.entity.SectionEntity;
import de.unistuttgart.iste.gits.content_service.persistence.repository.SectionRepository;
import de.unistuttgart.iste.gits.generated.dto.Section;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.util.HashSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@GraphQlApiTest
@TablesToDelete({"stage_required_contents", "stage_optional_contents", "stage", "section", "content_tags", "user_progress_data", "content"})
class MutationUpdateSectionTest {

    @Autowired
    private SectionRepository sectionRepository;

    @Test
    void testSectionUpdate(GraphQlTester tester){

        // fill database
        SectionEntity sectionEntity = SectionEntity.builder()
                .name("Test Section")
                .chapterId(UUID.randomUUID())
                .stages(new HashSet<>())
                .build();
        sectionEntity = sectionRepository.save(sectionEntity);

        String newName = "New Name";

        String query = """
                mutation ($id: UUID!, $name: String!){
                    mutateSection(sectionId: $id){
                        updateSectionName(name: $name){
                            id
                            name
                            chapterId
                            stages {
                                id                 
                            }
                        }
                    }
                    
                }
                """;

        SectionEntity finalSectionEntity = sectionEntity;

        tester.document(query)
                .variable("id", sectionEntity.getId())
                .variable("name", newName)
                .execute()
                .path("mutateSection.updateSectionName")
                .entity(Section.class)
                .satisfies(section -> {
                          assertEquals(finalSectionEntity.getId(), section.getId());
                    assertEquals(finalSectionEntity.getChapterId(), section.getChapterId());
                    assertEquals(newName, section.getName());
                    assertTrue(section.getStages().isEmpty());
                      }
                );
    }
}
