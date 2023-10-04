package de.unistuttgart.iste.gits.content_service.api.mutation;

import de.unistuttgart.iste.gits.common.testutil.*;
import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser.UserRoleInCourse;
import de.unistuttgart.iste.gits.content_service.persistence.entity.SectionEntity;
import de.unistuttgart.iste.gits.content_service.persistence.entity.StageEntity;
import de.unistuttgart.iste.gits.content_service.persistence.repository.SectionRepository;
import de.unistuttgart.iste.gits.content_service.persistence.repository.StageRepository;
import de.unistuttgart.iste.gits.generated.dto.Section;
import de.unistuttgart.iste.gits.generated.dto.Stage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.util.*;

import static de.unistuttgart.iste.gits.common.testutil.TestUsers.userWithMembershipInCourseWithId;
import static org.junit.jupiter.api.Assertions.assertEquals;

@GraphQlApiTest
@TablesToDelete({"stage_required_contents", "stage_optional_contents", "stage", "section", "content_tags", "user_progress_data", "content"})
class MutationChangeStageOrderTest {

    @Autowired
    SectionRepository sectionRepository;

    @Autowired
    StageRepository stageRepository;

    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser loggedInUser = userWithMembershipInCourseWithId(courseId, UserRoleInCourse.ADMINISTRATOR);

    @Test
    void testUpdateStageOrder(final GraphQlTester tester) {
        // set up database content and input
        final List<UUID> newStageOrderList = new ArrayList<>();

        SectionEntity sectionEntity = SectionEntity.builder()
                .name("Section test")
                .chapterId(UUID.randomUUID())
                .courseId(courseId)
                .stages(new HashSet<>())
                .build();

        sectionEntity = sectionRepository.save(sectionEntity);

        final Set<StageEntity> stageEntitySet = Set.of(
                buildStageEntity(sectionEntity.getId(), 0),
                buildStageEntity(sectionEntity.getId(), 1),
                buildStageEntity(sectionEntity.getId(), 2)
        );

        sectionEntity.setStages(stageEntitySet);

        sectionEntity = sectionRepository.save(sectionEntity);


        // reorder by putting the last element at the beginning and shifting each following element by one index
        for (int i = 0; i < 3; i++) {
            for (final StageEntity stageEntity : sectionEntity.getStages()) {
                if (stageEntity.getPosition() % 3 == i) {
                    newStageOrderList.add(stageEntity.getId());
                }
            }

        }

        executeUpdateStageOrderMutation(tester, sectionEntity.getId(), newStageOrderList)
                .path("mutateSection.updateStageOrder").entity(Section.class).satisfies(section -> {
                            assertEquals(3, section.getStages().size());
                            for (final Stage stage : section.getStages()) {
                                assertEquals(newStageOrderList.indexOf(stage.getId()), stage.getPosition());
                            }
                        }
                );
    }

    private GraphQlTester.Response executeUpdateStageOrderMutation(final GraphQlTester graphQlTester,
                                                                   final UUID sectionId,
                                                                   final List<UUID> stageList) {
        final String query = """
                mutation($sectionId: UUID!, $stageList: [UUID!]!){
                    mutateSection(sectionId: $sectionId){
                        updateStageOrder(stages: $stageList){
                            id
                            chapterId
                            name
                            stages {
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
                    
                }
                """;

        return graphQlTester.document(query)
                .variable("sectionId", sectionId)
                .variable("stageList", stageList)
                .execute();
    }

    private StageEntity buildStageEntity(final UUID sectionId, final int pos) {
        return StageEntity.builder()
                .id(UUID.randomUUID())
                .sectionId(sectionId)
                .position(pos)
                .requiredContents(new HashSet<>())
                .optionalContents(new HashSet<>())
                .build();
    }
}
