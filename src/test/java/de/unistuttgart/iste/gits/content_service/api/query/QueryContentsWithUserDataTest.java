package de.unistuttgart.iste.gits.content_service.api.query;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.TablesToDelete;
import de.unistuttgart.iste.gits.content_service.TestData;
import de.unistuttgart.iste.gits.content_service.persistence.dao.MediaContentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.dao.ProgressLogItemEmbeddable;
import de.unistuttgart.iste.gits.content_service.persistence.dao.UserProgressDataEntity;
import de.unistuttgart.iste.gits.content_service.persistence.repository.ContentRepository;
import de.unistuttgart.iste.gits.content_service.persistence.repository.UserProgressDataRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@GraphQlApiTest
@TablesToDelete({"content_tags", "user_progress_data_progress_log", "user_progress_data", "content", "tag"})
class QueryContentsWithUserDataTest {

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private UserProgressDataRepository userProgressDataRepository;

    @Test
    void testQueryWithUserDataForCurrentUser(HttpGraphQlTester graphQlTester) {
        // arrange one content object with two user data objects
        UUID chapterId = UUID.randomUUID();
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        MediaContentEntity contentEntity = TestData.dummyMediaContentEntityBuilder()
                .metadata(TestData.dummyContentMetadataEmbeddableBuilder()
                        .chapterId(chapterId)
                        .build())
                .build();
        contentEntity = contentRepository.save(contentEntity);

        String currentUser = """
                {
                    "id": "%s",
                    "userName": "MyUserName",
                    "firstName": "John",
                    "lastName": "Doe"
                }
                """.formatted(userId1.toString());

        UserProgressDataEntity userProgressDataEntity1 = UserProgressDataEntity.builder()
                .userId(userId1)
                .contentId(contentEntity.getId())
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
        userProgressDataRepository.save(userProgressDataEntity1);

        // create another to check if the query is filtered by user id
        UserProgressDataEntity userProgressDataEntity2 = UserProgressDataEntity.builder()
                .userId(userId2)
                .contentId(contentEntity.getId())
                .learningInterval(2)
                .progressLog(List.of())
                .build();
        userProgressDataRepository.save(userProgressDataEntity2);

        String query = """ 
                query {
                    contents {
                        elements {
                            id
                            metadata {
                                chapterId
                            }
                            userProgressData {
                                userId
                                learningInterval
                                log {
                                    timestamp
                                    success
                                    correctness
                                    hintsUsed
                                }
                            }
                        }
                    }
                }
                """;
        graphQlTester.mutate()
                .header("CurrentUser", currentUser)
                .build()
                .document(query)
                .execute()
                .path("contents.elements").entityList(Object.class).hasSize(1)
                .path("contents.elements[0].id").entity(UUID.class).isEqualTo(contentEntity.getId())
                .path("contents.elements[0].metadata.chapterId").entity(UUID.class).isEqualTo(chapterId)
                .path("contents.elements[0].userProgressData.userId").entity(UUID.class).isEqualTo(userId1)
                .path("contents.elements[0].userProgressData.learningInterval").entity(Integer.class).isEqualTo(1)
                .path("contents.elements[0].userProgressData.log[0].timestamp").entity(OffsetDateTime.class).satisfies(
                        offsetDateTime -> assertThat(
                                offsetDateTime.isEqual(OffsetDateTime.of(2021, 1, 2, 0, 0, 0, 0, ZoneOffset.ofHours(1))),
                                is(true)))
                .path("contents.elements[0].userProgressData.log[0].success").entity(Boolean.class).isEqualTo(false)
                .path("contents.elements[0].userProgressData.log[0].correctness").entity(Double.class).isEqualTo(1.0)
                .path("contents.elements[0].userProgressData.log[0].hintsUsed").entity(Integer.class).isEqualTo(0)
                .path("contents.elements[0].userProgressData.log[1].timestamp").entity(OffsetDateTime.class).satisfies(
                        offsetDateTime -> {
                            assertThat(
                                    offsetDateTime.isEqual(OffsetDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(1))),
                                    is(true));
                        })
                .path("contents.elements[0].userProgressData.log[1].success").entity(Boolean.class).isEqualTo(true)
                .path("contents.elements[0].userProgressData.log[1].correctness").entity(Double.class).isEqualTo(1.0)
                .path("contents.elements[0].userProgressData.log[1].hintsUsed").entity(Integer.class).isEqualTo(0)
                .path("contents.elements[0].userProgressData.log.length()").entity(Integer.class).isEqualTo(2);
    }

    @Test
    void testQueryWithUserDataForOtherUser(HttpGraphQlTester graphQlTester) {
        // arrange one content object with two user data objects
        UUID chapterId = UUID.randomUUID();
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        MediaContentEntity contentEntity = TestData.dummyMediaContentEntityBuilder()
                .metadata(TestData.dummyContentMetadataEmbeddableBuilder()
                        .chapterId(chapterId)
                        .build())
                .build();
        contentEntity = contentRepository.save(contentEntity);

        UserProgressDataEntity userProgressDataEntity1 = UserProgressDataEntity.builder()
                .userId(userId1)
                .contentId(contentEntity.getId())
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
        userProgressDataRepository.save(userProgressDataEntity1);

        // create another to check if the query is filtered by user id
        UserProgressDataEntity userProgressDataEntity2 = UserProgressDataEntity.builder()
                .userId(userId2)
                .contentId(contentEntity.getId())
                .learningInterval(2)
                .progressLog(List.of())
                .build();
        userProgressDataRepository.save(userProgressDataEntity2);

        String query = """ 
                query($userId: UUID!) {
                    contents {
                        elements {
                            id
                            metadata {
                                chapterId
                            }
                            progressDataForUser(userId: $userId) {
                                userId
                                learningInterval
                                log {
                                    timestamp
                                    success
                                    correctness
                                    hintsUsed
                                }
                            }
                        }
                    }
                }
                """;
        graphQlTester.document(query)
                .variable("userId", userId1)
                .execute()
                .path("contents.elements").entityList(Object.class).hasSize(1)
                .path("contents.elements[0].id").entity(UUID.class).isEqualTo(contentEntity.getId())
                .path("contents.elements[0].metadata.chapterId").entity(UUID.class).isEqualTo(chapterId)
                .path("contents.elements[0].progressDataForUser.userId").entity(UUID.class).isEqualTo(userId1)
                .path("contents.elements[0].progressDataForUser.learningInterval").entity(Integer.class).isEqualTo(1)
                .path("contents.elements[0].progressDataForUser.log[0].timestamp").entity(OffsetDateTime.class).satisfies(
                        offsetDateTime -> assertThat(
                                offsetDateTime.isEqual(OffsetDateTime.of(2021, 1, 2, 0, 0, 0, 0, ZoneOffset.ofHours(1))),
                                is(true)))
                .path("contents.elements[0].progressDataForUser.log[0].success").entity(Boolean.class).isEqualTo(false)
                .path("contents.elements[0].progressDataForUser.log[0].correctness").entity(Double.class).isEqualTo(1.0)
                .path("contents.elements[0].progressDataForUser.log[0].hintsUsed").entity(Integer.class).isEqualTo(0)
                .path("contents.elements[0].progressDataForUser.log[1].timestamp").entity(OffsetDateTime.class).satisfies(
                        offsetDateTime -> {
                            assertThat(
                                    offsetDateTime.isEqual(OffsetDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(1))),
                                    is(true));
                        })
                .path("contents.elements[0].progressDataForUser.log[1].success").entity(Boolean.class).isEqualTo(true)
                .path("contents.elements[0].progressDataForUser.log[1].correctness").entity(Double.class).isEqualTo(1.0)
                .path("contents.elements[0].progressDataForUser.log[1].hintsUsed").entity(Integer.class).isEqualTo(0)
                .path("contents.elements[0].progressDataForUser.log.length()").entity(Integer.class).isEqualTo(2);

    }
}
