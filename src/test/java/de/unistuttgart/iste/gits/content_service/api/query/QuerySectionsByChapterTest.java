package de.unistuttgart.iste.gits.content_service.api.query;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.TablesToDelete;
import de.unistuttgart.iste.gits.content_service.TestData;
import de.unistuttgart.iste.gits.content_service.persistence.entity.*;
import de.unistuttgart.iste.gits.content_service.persistence.mapper.SectionMapper;
import de.unistuttgart.iste.gits.content_service.persistence.repository.*;
import de.unistuttgart.iste.gits.generated.dto.Section;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@GraphQlApiTest
@TablesToDelete({"stage_required_contents", "stage_optional_contents", "stage", "section", "content_tags", "user_progress_data_progress_log", "user_progress_data", "content"})
class QuerySectionsByChapterTest {

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private StageRepository stageRepository;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private UserProgressDataRepository userProgressDataRepository;

    @Autowired
    private SectionMapper sectionMapper;

    private final UUID chapterId = UUID.randomUUID();
    private final UUID chapterId2 = UUID.randomUUID();

    private List<SectionEntity> fillDatabaseWithSections() {
        SectionEntity sectionEntity = SectionEntity.builder()
                .name("Test Section")
                .chapterId(chapterId)
                .stages(new HashSet<>())
                .build();
        SectionEntity sectionEntity2 = SectionEntity.builder()
                .name("Test Section2")
                .chapterId(chapterId)
                .stages(new HashSet<>())
                .build();
        SectionEntity sectionEntity3 = SectionEntity.builder()
                .name("Test Section3")
                .chapterId(chapterId2)
                .stages(new HashSet<>())
                .build();

        sectionEntity = sectionRepository.save(sectionEntity);
        sectionEntity2 = sectionRepository.save(sectionEntity2);
        sectionEntity3 = sectionRepository.save(sectionEntity3);

        StageEntity stageEntity = StageEntity.builder()
                .sectionId(sectionEntity.getId())
                .position(0)
                .optionalContents(new HashSet<>())
                .requiredContents(new HashSet<>())
                .build();
        StageEntity stageEntity2 = StageEntity.builder()
                .sectionId(sectionEntity2.getId())
                .position(0)
                .optionalContents(new HashSet<>())
                .requiredContents(new HashSet<>())
                .build();
        stageEntity = stageRepository.save(stageEntity);
        stageEntity2 = stageRepository.save(stageEntity2);

        sectionEntity.getStages().add(stageEntity);
        sectionEntity2.getStages().add(stageEntity2);

        sectionEntity = sectionRepository.save(sectionEntity);
        sectionEntity2 = sectionRepository.save(sectionEntity2);

        return List.of(sectionEntity, sectionEntity2, sectionEntity3);
    }

    private List<MediaContentEntity> fillDatabaseWithContent() {
        final List<MediaContentEntity> contentEntities = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            MediaContentEntity contentEntity = TestData.dummyMediaContentEntityBuilder()
                    .metadata(TestData.dummyContentMetadataEmbeddableBuilder()
                            .chapterId(chapterId)
                            .build())
                    .build();
            contentEntity = contentRepository.save(contentEntity);
            contentEntities.add(contentEntity);
        }
        return contentEntities;
    }

    private List<UserProgressDataEntity> fillDatabaseWithUserProgress(final UUID userId, final List<MediaContentEntity> contentEntities) {
        final UserProgressDataEntity userProgressDataEntity1 = UserProgressDataEntity.builder()
                .userId(userId)
                .contentId(contentEntities.get(0).getId())
                .learningInterval(1)
                .progressLog(List.of(
                        ProgressLogItemEmbeddable.builder()
                                .success(true)
                                .correctness(1.0)
                                .hintsUsed(0)
                                .timestamp(LocalDate.of(2021, 1, 1)
                                        .atStartOfDay()
                                        .atOffset(ZoneOffset.ofHours(1)))
                                .build(),
                        ProgressLogItemEmbeddable.builder()
                                .success(false)
                                .correctness(1.0)
                                .hintsUsed(0)
                                .timestamp(LocalDate.of(2021, 1, 2)
                                        .atStartOfDay()
                                        .atOffset(ZoneOffset.ofHours(1)))
                                .build()
                ))
                .build();

        final UserProgressDataEntity userProgressDataEntity2 = UserProgressDataEntity.builder()
                .userId(userId)
                .contentId(contentEntities.get(1).getId())
                .learningInterval(1)
                .progressLog(List.of(
                        ProgressLogItemEmbeddable.builder()
                                .success(false)
                                .correctness(0.5)
                                .hintsUsed(0)
                                .timestamp(LocalDate.of(2021, 1, 1)
                                        .atStartOfDay()
                                        .atOffset(ZoneOffset.ofHours(1)))
                                .build(),
                        ProgressLogItemEmbeddable.builder()
                                .success(false)
                                .correctness(0.5)
                                .hintsUsed(0)
                                .timestamp(LocalDate.of(2021, 1, 2)
                                        .atStartOfDay()
                                        .atOffset(ZoneOffset.ofHours(1)))
                                .build()
                ))
                .build();

        final UserProgressDataEntity userProgressDataEntity3 = UserProgressDataEntity.builder()
                .userId(userId)
                .contentId(contentEntities.get(2).getId())
                .learningInterval(1)
                .progressLog(List.of(
                        ProgressLogItemEmbeddable.builder()
                                .success(false)
                                .correctness(0.5)
                                .hintsUsed(0)
                                .timestamp(LocalDate.of(2021, 1, 1)
                                        .atStartOfDay()
                                        .atOffset(ZoneOffset.ofHours(1)))
                                .build(),
                        ProgressLogItemEmbeddable.builder()
                                .success(false)
                                .correctness(0.5)
                                .hintsUsed(0)
                                .timestamp(LocalDate.of(2021, 1, 2)
                                        .atStartOfDay()
                                        .atOffset(ZoneOffset.ofHours(1)))
                                .build()
                ))
                .build();

        final List<UserProgressDataEntity> progressDataEntities = List.of(userProgressDataEntity1, userProgressDataEntity2, userProgressDataEntity3);

        userProgressDataRepository.saveAll(progressDataEntities);

        return progressDataEntities;
    }

    @Test
    void testQuerySectionsByChapter(final GraphQlTester tester) {
        final List<SectionEntity> entities = fillDatabaseWithSections();
        final List<Section> entitiesMapped = entities.stream().map(sectionMapper::entityToDto).toList();

        final String query = """
                query($chapterIds: [UUID!]!) {
                sectionsByChapterIds(chapterIds: $chapterIds){
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
                """;

        final ParameterizedTypeReference<List<Section>> sectionListType = new ParameterizedTypeReference<List<Section>>() {
        };

        final List<List<Section>> result = tester.document(query)
                .variable("chapterIds", List.of(chapterId, chapterId2))
                .execute()
                .path("sectionsByChapterIds").entityList(sectionListType).get();

        assertThat(result.get(0)).containsExactlyInAnyOrder(entitiesMapped.get(0), entitiesMapped.get(1));
        assertThat(result.get(1)).containsExactlyInAnyOrder(entitiesMapped.get(2));

        //clean up
        sectionRepository.deleteAll();
    }

    @Test
    void testQuerySectionsByChapterWithUserData(final HttpGraphQlTester graphQlTester) {
        final UUID userId = UUID.randomUUID();

        //init database Data
        final List<SectionEntity> entities = fillDatabaseWithSections();
        final List<MediaContentEntity> contentEntities = fillDatabaseWithContent();
        final List<UserProgressDataEntity> userProgressDataEntities = fillDatabaseWithUserProgress(userId, contentEntities);

        // link some content to stages
        final SectionEntity section = entities.get(0);
        section.getStages().forEach(stageEntity -> {
            stageEntity.getRequiredContents().add(contentEntities.get(0));
            stageEntity.getRequiredContents().add(contentEntities.get(1));
            stageEntity.getOptionalContents().add(contentEntities.get(2));
        });
        sectionRepository.save(section);


        final String currentUser = """
                {
                    "id": "%s",
                    "userName": "MyUserName",
                    "firstName": "John",
                    "lastName": "Doe",
                    "courseMemberships": []
                }
                """.formatted(userId.toString());


        final String query = """
                query($chapterIds: [UUID!]!) {
                sectionsByChapterIds(chapterIds: $chapterIds){
                    id
                    chapterId
                    name
                    stages {
                        id
                        position
                        requiredContents {
                            id
                            }
                        requiredContentsProgress    
                        optionalContents {
                            id
                            }
                        optionalContentsProgress    
                        }
                    }
                }
                """;

        graphQlTester.mutate()
                .header("CurrentUser", currentUser)
                .build()
                .document(query)
                .variable("chapterIds", List.of(chapterId))
                .execute()
                .path("sectionsByChapterIds[0]").entityList(Object.class).hasSize(2)
                .path("sectionsByChapterIds[0][0].id").entity(UUID.class).isEqualTo(entities.get(0).getId())
                .path("sectionsByChapterIds[0][0].chapterId").entity(UUID.class).isEqualTo(entities.get(0).getChapterId())
                .path("sectionsByChapterIds[0][0].name").entity(String.class).isEqualTo("Test Section")
                .path("sectionsByChapterIds[0][0].stages").entityList(Object.class).hasSize(1)
                .path("sectionsByChapterIds[0][0].stages[0].requiredContents").entityList(Object.class).hasSize(2)
                .path("sectionsByChapterIds[0][0].stages[0].requiredContentsProgress").entity(Double.class).isEqualTo(50.0)
                .path("sectionsByChapterIds[0][0].stages[0].optionalContents").entityList(Object.class).hasSize(1)
                .path("sectionsByChapterIds[0][0].stages[0].optionalContentsProgress").entity(Double.class).isEqualTo(0.0)
                .path("sectionsByChapterIds[0][1].id").entity(UUID.class).isEqualTo(entities.get(1).getId())
                .path("sectionsByChapterIds[0][1].chapterId").entity(UUID.class).isEqualTo(entities.get(1).getChapterId())
                .path("sectionsByChapterIds[0][1].name").entity(String.class).isEqualTo("Test Section2")
                .path("sectionsByChapterIds[0][1].stages").entityList(Object.class).hasSize(1)
                .path("sectionsByChapterIds[0][1].stages[0].requiredContents").entityList(Object.class).hasSize(0)
                .path("sectionsByChapterIds[0][1].stages[0].requiredContentsProgress").entity(Double.class).isEqualTo(100.0)
                .path("sectionsByChapterIds[0][1].stages[0].optionalContents").entityList(Object.class).hasSize(0)
                .path("sectionsByChapterIds[0][1].stages[0].optionalContentsProgress").entity(Double.class).isEqualTo(100.0);


        //clean up
        sectionRepository.deleteAll();
    }
}
