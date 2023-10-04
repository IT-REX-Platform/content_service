package de.unistuttgart.iste.gits.content_service.api.mutation;

import de.unistuttgart.iste.gits.common.testutil.*;
import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser.UserRoleInCourse;
import de.unistuttgart.iste.gits.content_service.persistence.entity.SectionEntity;
import de.unistuttgart.iste.gits.content_service.persistence.repository.SectionRepository;
import de.unistuttgart.iste.gits.generated.dto.CreateSectionInput;
import de.unistuttgart.iste.gits.generated.dto.Section;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.annotation.Commit;

import java.util.List;
import java.util.UUID;

import static de.unistuttgart.iste.gits.common.testutil.TestUsers.userWithMembershipInCourseWithId;
import static org.junit.jupiter.api.Assertions.*;

@GraphQlApiTest
@TablesToDelete({"stage_required_contents", "stage_optional_contents", "stage", "section", "content_tags", "user_progress_data", "content"})
class MutationCreateSectionTest {

    @Autowired
    private SectionRepository sectionRepository;

    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser loggedInUser = userWithMembershipInCourseWithId(courseId, UserRoleInCourse.ADMINISTRATOR);

    @Test
    @Transactional
    @Commit
    void testSectionCreation(final GraphQlTester tester) {
        final CreateSectionInput input = CreateSectionInput.builder()
                .setChapterId(UUID.randomUUID())
                .setName("Test Section")
                .build();

        executeCreateSection(tester, input)
                .path("_internal_createSection")
                .entity(Section.class).satisfies(workPath -> {
                    assertNotNull(workPath.getId());
                    assertEquals(input.getName(), workPath.getName());
                    assertEquals(input.getChapterId() ,workPath.getChapterId());
                    assertTrue(workPath.getStages().isEmpty());
                });

        final List<SectionEntity> sectionEntities = sectionRepository.findAll();
        assertEquals(1, sectionEntities.size());
        final SectionEntity sectionEntity = sectionEntities.get(0);

        assertEquals(input.getName(), sectionEntity.getName());
        assertEquals(input.getChapterId(), sectionEntity.getChapterId());
        assertEquals(0, sectionEntity.getPosition());
        assertTrue(sectionEntity.getStages().isEmpty());
    }

    private GraphQlTester.Response executeCreateSection(final GraphQlTester graphQlTester, final CreateSectionInput input) {
        final String query = """
                mutation ($input: CreateSectionInput!, $courseId: UUID!) {
                    _internal_createSection(input: $input, courseId: $courseId) {
                        id
                        name
                        chapterId
                        stages {
                            id
                            position
                            optionalContents {
                                id
                            }
                            requiredContents {
                                id
                            }
                        }
                    }
                }
                """;
        return graphQlTester.document(query)
                .variable("input", input)
                .variable("courseId", courseId)
                .execute();
    }
}
