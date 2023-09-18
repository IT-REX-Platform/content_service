package de.unistuttgart.iste.gits.content_service.api.query;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.TablesToDelete;
import de.unistuttgart.iste.gits.content_service.TestData;
import de.unistuttgart.iste.gits.content_service.persistence.entity.MediaContentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.repository.ContentRepository;
import de.unistuttgart.iste.gits.content_service.persistence.repository.UserProgressDataRepository;
import de.unistuttgart.iste.gits.generated.dto.CompositeProgressInformation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

import java.util.List;
import java.util.UUID;

import static graphql.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@GraphQlApiTest
@TablesToDelete({"content_tags", "user_progress_data_progress_log", "user_progress_data", "content"})
class QueryProgressByChapterIdsTest {

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private UserProgressDataRepository userProgressDataRepository;


    /**
     * This Testcase assumes Progress has already been made for all content within a chapter
     *
     * @param graphQlTester GraphQL HTTP Tester, which allows for manipulation of the HTTP header we require for User-Information
     */
    @Test
    void testProgressByChapterId(HttpGraphQlTester graphQlTester) {
        UUID userId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        MediaContentEntity mediaContentEntity = contentRepository.save(TestData.buildContentEntity(chapterId));
        MediaContentEntity mediaContentEntity1 = contentRepository.save(TestData.buildContentEntity(chapterId));

        userProgressDataRepository.save(TestData.buildDummyUserProgressData(true, userId, mediaContentEntity.getId()));
        userProgressDataRepository.save(TestData.buildDummyUserProgressData(false, userId, mediaContentEntity1.getId()));

        String currentUser = """
                {
                    "id": "%s",
                    "userName": "MyUserName",
                    "firstName": "John",
                    "lastName": "Doe",
                    "courseMemberships": []
                }
                """.formatted(userId.toString());

        String query = """ 
                query($chapterIds: [UUID!]!) {
                    progressByChapterIds(chapterIds: $chapterIds){
                        progress
                        completedContents
                        totalContents
                    }
                }
                """;
        List<CompositeProgressInformation> resultList = graphQlTester.mutate()
                .header("CurrentUser", currentUser)
                .build()
                .document(query)
                .variable("chapterIds", List.of(chapterId))
                .execute()
                .path("progressByChapterIds").entityList(CompositeProgressInformation.class).get();

        assertEquals(1, resultList.size());

        CompositeProgressInformation resultItem = resultList.get(0);
        assertEquals(50.0, resultItem.getProgress());
        assertEquals(1, resultItem.getCompletedContents());
        assertEquals(2, resultItem.getTotalContents());
    }

    /**
     * This Testcase assumes no Progress has already been made for all content within a chapter
     *
     * @param graphQlTester GraphQL HTTP Tester, which allows for manipulation of the HTTP header we require for User-Information
     */
    @Test
    void testProgressByChapterIdWithNoProgress(HttpGraphQlTester graphQlTester) {
        UUID userId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();
        MediaContentEntity mediaContentEntity = contentRepository.save(TestData.buildContentEntity(chapterId));
        MediaContentEntity mediaContentEntity1 = contentRepository.save(TestData.buildContentEntity(chapterId));


        String currentUser = """
                {
                    "id": "%s",
                    "userName": "MyUserName",
                    "firstName": "John",
                    "lastName": "Doe",
                    "courseMemberships": []
                }
                """.formatted(userId.toString());

        String query = """ 
                query($chapterIds: [UUID!]!) {
                    progressByChapterIds(chapterIds: $chapterIds){
                        progress
                        completedContents
                        totalContents
                    }
                }
                """;
        List<CompositeProgressInformation> resultList = graphQlTester.mutate()
                .header("CurrentUser", currentUser)
                .build()
                .document(query)
                .variable("chapterIds", List.of(chapterId))
                .execute()
                .path("progressByChapterIds").entityList(CompositeProgressInformation.class).get();

        assertEquals(1, resultList.size());

        CompositeProgressInformation resultItem = resultList.get(0);
        assertEquals(0.0, resultItem.getProgress());
        assertEquals(0, resultItem.getCompletedContents());
        assertEquals(2, resultItem.getTotalContents());
    }

    /**
     * This Testcase assumes no content exists for the chapter
     *
     * @param graphQlTester GraphQL HTTP Tester, which allows for manipulation of the HTTP header we require for User-Information
     */
    @Test
    void testProgressByChapterIdWithNoContent(HttpGraphQlTester graphQlTester) {
        UUID userId = UUID.randomUUID();
        UUID chapterId = UUID.randomUUID();

        String currentUser = """
                {
                    "id": "%s",
                    "userName": "MyUserName",
                    "firstName": "John",
                    "lastName": "Doe",
                    "courseMemberships": []
                }
                """.formatted(userId.toString());

        String query = """ 
                query($chapterIds: [UUID!]!) {
                    progressByChapterIds(chapterIds: $chapterIds){
                        progress
                        completedContents
                        totalContents
                    }
                }
                """;
        List<CompositeProgressInformation> resultList = graphQlTester.mutate()
                .header("CurrentUser", currentUser)
                .build()
                .document(query)
                .variable("chapterIds", List.of(chapterId))
                .execute()
                .path("progressByChapterIds").entityList(CompositeProgressInformation.class).get();

        assertTrue(resultList.isEmpty());

    }
}
