package de.unistuttgart.iste.gits.content_service.api.mutation;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.TablesToDelete;
import de.unistuttgart.iste.gits.content_service.persistence.entity.SectionEntity;
import de.unistuttgart.iste.gits.content_service.persistence.repository.SectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.util.HashSet;
import java.util.UUID;

@GraphQlApiTest
@TablesToDelete({"stage_required_contents", "stage_optional_contents", "stage", "section", "content_tags", "user_progress_data", "content"})
class MutationDeleteSectionTest {

    @Autowired
    private SectionRepository sectionRepository;

    @Test

    void testSectionDeletion(final GraphQlTester tester){
        SectionEntity sectionEntity = SectionEntity.builder()
                .name("Test Section")
                .chapterId(UUID.randomUUID())
                .stages(new HashSet<>())
                .build();
        sectionEntity = sectionRepository.save(sectionEntity);

        final String query = """
                mutation ($id: UUID!){
                    mutateSection(sectionId: $id){
                        deleteSection
                    }
                }
                """;
        tester.document(query)
                .variable("id", sectionEntity.getId())
                .execute()
                .path("mutateSection.deleteSection")
                .entity(UUID.class)
                .isEqualTo(sectionEntity.getId());
    }
}
