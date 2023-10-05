package de.unistuttgart.iste.gits.content_service.api.query;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.gits.common.testutil.TablesToDelete;
import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser;
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

import static de.unistuttgart.iste.gits.common.testutil.TestUsers.userWithMembershipInCourseWithId;
import static org.junit.jupiter.api.Assertions.assertEquals;

@GraphQlApiTest
@TablesToDelete({"content_tags", "user_progress_data_progress_log", "user_progress_data", "content"})
class QueryProgressByChapterIdsTest {

    private static final String QUERY_USER_PROGRESS_BY_CHAPTER_IDS = """ 
            query($chapterIds: [UUID!]!) {
                _internal_noauth_progressByChapterIds(chapterIds: $chapterIds){
                    progress
                    completedContents
                    totalContents
                }
            }
            """;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private UserProgressDataRepository userProgressDataRepository;

    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser loggedInUser = userWithMembershipInCourseWithId(courseId, LoggedInUser.UserRoleInCourse.STUDENT);


    /**
     * This Testcase assumes Progress has already been made for all content within a chapter
     */
    @Test
    void testProgressByChapterId(HttpGraphQlTester graphQlTester) {
        UUID userId = loggedInUser.getId();
        UUID chapterId = UUID.randomUUID();
        MediaContentEntity mediaContentEntity = contentRepository.save(TestData.buildContentEntity(chapterId));
        MediaContentEntity mediaContentEntity1 = contentRepository.save(TestData.buildContentEntity(chapterId));

        userProgressDataRepository.save(TestData.buildDummyUserProgressData(true, userId, mediaContentEntity.getId()));
        userProgressDataRepository.save(TestData.buildDummyUserProgressData(false, userId, mediaContentEntity1.getId()));

        List<CompositeProgressInformation> resultList
                = executeProgressByChapterIdsQuery(graphQlTester, chapterId);

        CompositeProgressInformation resultItem = resultList.get(0);
        assertEquals(50.0, resultItem.getProgress());
        assertEquals(1, resultItem.getCompletedContents());
        assertEquals(2, resultItem.getTotalContents());
    }

    /**
     * This Testcase assumes no Progress has already been made for all content within a chapter
     */
    @Test
    void testProgressByChapterIdWithNoProgress(HttpGraphQlTester graphQlTester) {
        UUID chapterId = UUID.randomUUID();
        contentRepository.save(TestData.buildContentEntity(chapterId));
        contentRepository.save(TestData.buildContentEntity(chapterId));


        List<CompositeProgressInformation> resultList
                = executeProgressByChapterIdsQuery(graphQlTester, chapterId);

        assertEquals(1, resultList.size());

        CompositeProgressInformation resultItem = resultList.get(0);
        assertEquals(0.0, resultItem.getProgress());
        assertEquals(0, resultItem.getCompletedContents());
        assertEquals(2, resultItem.getTotalContents());
    }

    /**
     * This Testcase assumes no content exists for the chapter
     */
    @Test
    void testProgressByChapterIdWithNoContent(HttpGraphQlTester graphQlTester) {
        UUID chapterId = UUID.randomUUID();

        List<CompositeProgressInformation> resultList
                = executeProgressByChapterIdsQuery(graphQlTester, chapterId);

        assertEquals(1, resultList.size());

        CompositeProgressInformation resultItem = resultList.get(0);
        assertEquals(100.0, resultItem.getProgress());
        assertEquals(0, resultItem.getCompletedContents());
        assertEquals(0, resultItem.getTotalContents());
    }

    private List<CompositeProgressInformation> executeProgressByChapterIdsQuery(HttpGraphQlTester graphQlTester,
                                                                                UUID chapterId) {


        return graphQlTester
                .document(QUERY_USER_PROGRESS_BY_CHAPTER_IDS)
                .variable("chapterIds", List.of(chapterId))
                .execute()
                .path("_internal_noauth_progressByChapterIds")
                .entityList(CompositeProgressInformation.class).get();
    }
}
