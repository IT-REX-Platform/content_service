package de.unistuttgart.iste.gits.content_service.api.mutation;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.TablesToDelete;
import de.unistuttgart.iste.gits.content_service.persistence.dao.ContentMetadataEmbeddable;
import de.unistuttgart.iste.gits.content_service.persistence.dao.MediaContentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.dao.SectionEntity;
import de.unistuttgart.iste.gits.content_service.persistence.dao.StageEntity;
import de.unistuttgart.iste.gits.content_service.persistence.repository.ContentRepository;
import de.unistuttgart.iste.gits.content_service.persistence.repository.SectionRepository;
import de.unistuttgart.iste.gits.content_service.persistence.repository.StageRepository;
import de.unistuttgart.iste.gits.generated.dto.ContentType;
import de.unistuttgart.iste.gits.generated.dto.UpdateStageInput;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.annotation.Commit;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@GraphQlApiTest
@TablesToDelete({"stage_required_contents", "stage_optional_content", "stage" ,"section" , "content_tags", "user_progress_data", "content", "tag"})
class MutationUpdateStageTest {

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private StageRepository stageRepository;

    @Autowired
    private ContentRepository contentRepository;

    @Test
    @Transactional
    @Commit
    void testUpdateStage(GraphQlTester tester){
        List<UUID> contentIds = new ArrayList<>();
        SectionEntity sectionEntity = SectionEntity.builder()
                .name("Test Section")
                .chapterId(UUID.randomUUID())
                .stages(new HashSet<>())
                .build();
        sectionEntity = sectionRepository.save(sectionEntity);

        StageEntity stageEntity = StageEntity.builder()
                .sectionId(sectionEntity.getId())
                .position(0)
                .optionalContents(new HashSet<>())
                .requiredContents(new HashSet<>())
                .build();
        stageEntity = stageRepository.save(stageEntity);


        for (int i = 0; i < 2; i++) {
            MediaContentEntity entity = buildContentEntity(sectionEntity.getChapterId());
            entity = contentRepository.save(entity);
            contentIds.add(entity.getId());
        }

        UpdateStageInput input = UpdateStageInput.builder()
                .setId(stageEntity.getId())
                .setRequiredContents(List.of(contentIds.get(0)))
                .setOptionalContents(List.of(contentIds.get(1)))
                .build();

        String query = """
                mutation ($input: UpdateStageInput!){
                    updateStage(input: $input){
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
                """;

        String expectedJson = """
                      {
                      "id": "%s",
                      "position": 0,
                      "requiredContents": [
                        {
                          "id": "%s"
                        }
                      ],
                      "optionalContents": [
                        {
                          "id": "%s"
                        }
                      ]
                    }
                """.formatted(stageEntity.getId(), contentIds.get(0), contentIds.get(1));

        tester.document(query)
                .variable("input", input)
                .execute().path("updateStage").matchesJson(expectedJson);

    }

    private MediaContentEntity buildContentEntity(UUID chapterId){
        return MediaContentEntity.builder()
                .id(UUID.randomUUID())
                .metadata(
                        ContentMetadataEmbeddable.builder()
                                .tags(new HashSet<>())
                                .name("Test")
                                .type(ContentType.MEDIA)
                                .suggestedDate(OffsetDateTime.parse("2020-01-01T00:00:00.000Z"))
                                .rewardPoints(20)
                                .chapterId(chapterId)
                                .build()
                ).build();
    }
}
