package de.unistuttgart.iste.gits.content_service.api.query;

import de.unistuttgart.iste.gits.common.testutil.*;
import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.gits.content_service.persistence.entity.*;
import de.unistuttgart.iste.gits.content_service.persistence.repository.SectionRepository;
import de.unistuttgart.iste.gits.content_service.persistence.repository.StageRepository;
import de.unistuttgart.iste.gits.generated.dto.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.HttpGraphQlTester;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import static de.unistuttgart.iste.gits.common.testutil.TestUsers.userWithMembershipInCourseWithId;

/**
 * Basic test for the suggestions query, detailed tests are in the SuggestionsServiceTest.
 */
@GraphQlApiTest
@TablesToDelete({"stage_required_contents", "stage_optional_contents", "stage", "section", "content_tags", "user_progress_data_progress_log", "user_progress_data", "content"})
class QuerySuggestionsTest {

    @Autowired
    private SectionRepository sectionRepository;
    @Autowired
    private StageRepository stageRepository;

    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser loggedInUser = userWithMembershipInCourseWithId(courseId, LoggedInUser.UserRoleInCourse.STUDENT);

    /**
     * Given a user with progress
     * When the suggestions query is called
     * Then the suggestions are returned
     */
    @Test
    void testSuggestions(final HttpGraphQlTester graphQlTester) {
        final UUID chapterId = UUID.randomUUID();

        // Arrange
        final SectionEntity testSection = sectionRepository.save(SectionEntity.builder()
                .name("Test Section")
                .chapterId(chapterId)
                .courseId(courseId)
                .stages(Set.of())
                .build());

        stageRepository.save(
                StageEntity.builder()
                        .position(1)
                        .sectionId(testSection.getId())
                        .requiredContents(Set.of(
                                ContentEntity.builder()
                                        .metadata(ContentMetadataEmbeddable.builder()
                                                .type(ContentType.MEDIA)
                                                .suggestedDate(OffsetDateTime.now().minusDays(1))
                                                .name("ContentDue1")
                                                .chapterId(chapterId)
                                                .courseId(courseId)
                                                .build())
                                        .build(),
                                ContentEntity.builder()
                                        .metadata(ContentMetadataEmbeddable.builder()
                                                .type(ContentType.MEDIA)
                                                .suggestedDate(OffsetDateTime.now().minusDays(2))
                                                .name("ContentDue2")
                                                .chapterId(chapterId)
                                                .courseId(courseId)
                                                .build())
                                        .build(),
                                ContentEntity.builder()
                                        .metadata(ContentMetadataEmbeddable.builder()
                                                .type(ContentType.MEDIA)
                                                .suggestedDate(OffsetDateTime.now().minusDays(3))
                                                .name("ContentDue3")
                                                .chapterId(chapterId)
                                                .courseId(courseId)
                                                .build())
                                        .build()
                        ))
                        .optionalContents(Set.of())
                        .build());

        final String query = """
                query($chapterIds: [UUID!]!) {
                    suggestionsByChapterIds(chapterIds: $chapterIds, amount: 2) {
                        type
                        content {
                           metadata {
                              name
                           }
                        }
                    }
                }
                """;

        graphQlTester
                .document(query)
                .variable("chapterIds", Set.of(chapterId))
                .execute()
                .path("suggestionsByChapterIds[*].content.metadata.name")
                .entityList(String.class)
                .containsExactly("ContentDue3", "ContentDue2");
    }
}
