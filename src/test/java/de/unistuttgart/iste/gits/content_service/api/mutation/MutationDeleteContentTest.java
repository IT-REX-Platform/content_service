package de.unistuttgart.iste.gits.content_service.api.mutation;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.TablesToDelete;
import de.unistuttgart.iste.gits.content_service.TestData;
import de.unistuttgart.iste.gits.content_service.dapr.TopicPublisher;
import de.unistuttgart.iste.gits.content_service.persistence.entity.*;
import de.unistuttgart.iste.gits.content_service.persistence.repository.*;
import de.unistuttgart.iste.gits.content_service.test_config.MockTopicPublisherConfiguration;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ContextConfiguration;

import java.util.*;

import static graphql.Assert.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;


@ContextConfiguration(classes = MockTopicPublisherConfiguration.class)
@GraphQlApiTest
@TablesToDelete({"content_tags", "user_progress_data", "content"})
class MutationDeleteContentTest {

    @Autowired
    private ContentRepository contentRepository;
    @Autowired
    private UserProgressDataRepository userProgressRepository;
    @Autowired
    private StageRepository stageRepository;
    @Autowired
    private SectionRepository sectionRepository;
    @Autowired
    private TopicPublisher topicPublisher;


    /**
     * Given a UUID of an existing content
     * When the deleteContent mutation is executed
     * Then the content is deleted
     */
    @Test
    @Transactional
    @Commit
    void testDeleteExistingContent(GraphQlTester graphQlTester) {
        ContentEntity contentEntity = contentRepository.save(TestData.dummyAssessmentEntityBuilder()
                .metadata(TestData.dummyContentMetadataEmbeddableBuilder()
                        .tags(new HashSet<>(Set.of("Tag", "Tag2")))
                        .build())
                .build());
        contentEntity = contentRepository.save(contentEntity);

        UserProgressDataEntity progress1 = UserProgressDataEntity.builder()
                .contentId(contentEntity.getId())
                .userId(UUID.randomUUID())
                .learningInterval(2)
                .build();
        progress1 = userProgressRepository.save(progress1);

        UserProgressDataEntity progress2 = UserProgressDataEntity.builder()
                .contentId(contentEntity.getId())
                .userId(UUID.randomUUID())
                .learningInterval(1)
                .build();
        progress2 = userProgressRepository.save(progress2);



        String query = """
                mutation($id: UUID!) {
                    mutateContent(contentId: $id){
                        deleteContent
                    }
                }
                """;

        graphQlTester.document(query)
                .variable("id", contentEntity.getId())
                .execute()
                .path("mutateContent.deleteContent").entity(UUID.class).isEqualTo(contentEntity.getId());

        // test that content is deleted
        assertThat(contentRepository.count(), is(0L));

        // test that user progress is deleted
        assertThat(userProgressRepository.count(), is(0L));

    }


    /**
     * Given a UUID of an existing content
     * When the deleteContent mutation is executed
     * Then the content is deleted and all links to stages are removed
     */
    @Test
    @Transactional
    @Commit
    void testDeleteExistingContentLinkedToStage(GraphQlTester graphQlTester) {

        ContentEntity contentEntity = contentRepository.save(TestData.dummyAssessmentEntityBuilder()
                .metadata(TestData.dummyContentMetadataEmbeddableBuilder()
                        .tags(Set.of("Tag3", "Tag4"))
                        .build())
                .build());

        UserProgressDataEntity progress1 = userProgressRepository.save(UserProgressDataEntity.builder()
                .contentId(contentEntity.getId())
                .userId(UUID.randomUUID())
                .learningInterval(1)
                .build());

        UserProgressDataEntity progress2 = userProgressRepository.save(UserProgressDataEntity.builder()
                .contentId(contentEntity.getId())
                .userId(UUID.randomUUID())
                .learningInterval(2)
                .build());

        // add Section and Stage to db and link content to a stage
        SectionEntity sectionEntity = sectionRepository.save(SectionEntity.builder().stages(new HashSet<>()).name("TestSection").chapterId(UUID.randomUUID()).build());
        StageEntity stageEntity = StageEntity.builder().sectionId(sectionEntity.getId()).position(0).requiredContents(new HashSet<>()).optionalContents(new HashSet<>()).build();
        stageEntity.getRequiredContents().add(contentEntity);
        stageEntity = stageRepository.save(stageEntity);

        String query = """
                mutation($id: UUID!) {
                    mutateContent(contentId: $id){
                        deleteContent
                    }
                }
                """;

        graphQlTester.document(query)
                .variable("id", contentEntity.getId())
                .execute()
                .path("mutateContent.deleteContent").entity(UUID.class).isEqualTo(contentEntity.getId());

        assertThat(contentRepository.findById(contentEntity.getId()).isEmpty(), is(true));
        System.out.println(contentRepository.findAll());
        assertThat(contentRepository.count(), is(0L));

        // assert content has been unlinked from Stages
        stageEntity = stageRepository.getReferenceById(stageEntity.getId());
        assertFalse(stageEntity.getRequiredContents().contains(contentEntity));

        // assert user progress has been deleted
        assertThat(userProgressRepository.count(), is(0L));
    }

    /**
     * Given a UUID of a non-existing content
     * When the deleteContent mutation is executed
     * Then an error is returned
     */
    @Test
    void testDeleteNonExistingContent(GraphQlTester graphQlTester) {
        UUID id = UUID.randomUUID();
        String query = """
                mutation($id: UUID!) {
                    mutateContent(contentId: $id){
                        deleteContent
                    } 
                }
                """;

        graphQlTester.document(query)
                .variable("id", id)
                .execute()
                .errors()
                .satisfy(responseErrors -> {
                    assertThat(responseErrors.size(), is(1));
                    assertThat(responseErrors.get(0).getExtensions().get("classification"),
                            is("DataFetchingException"));
                    assertThat(responseErrors.get(0).getMessage(),
                            containsString("Content with id " + id + " not found"));
                });

    }
}
