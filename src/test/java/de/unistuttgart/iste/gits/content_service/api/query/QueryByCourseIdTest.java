package de.unistuttgart.iste.gits.content_service.api.query;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser.UserRoleInCourse;
import de.unistuttgart.iste.gits.content_service.TestData;
import de.unistuttgart.iste.gits.content_service.persistence.entity.ContentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.repository.ContentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.util.List;
import java.util.UUID;

import static de.unistuttgart.iste.gits.common.testutil.TestUsers.userWithMembershipInCourseWithId;

@GraphQlApiTest
class QueryByCourseIdTest {

    @Autowired
    private ContentRepository contentRepository;

    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser loggedInUser = userWithMembershipInCourseWithId(courseId, UserRoleInCourse.STUDENT);

    /**
     * Given valid courseIds
     * When the queryByCourseId query is called
     * Then the content is returned, correctly grouped and filtered by courseId
     */
    @Test
    void testQueryByCourseId(final GraphQlTester graphQlTester) {
        final List<UUID> courseIds = List.of(courseId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        courseIds.stream()
                .<ContentEntity>map(courseId -> (
                        TestData.dummyMediaContentEntityBuilder(courseId)
                                .metadata(TestData.dummyContentMetadataEmbeddableBuilder(courseId)
                                        .courseId(courseId)
                                        .build()))
                        .build())
                .forEach(contentRepository::save);

        final String query = """
                query($courseIds: [UUID!]!) {
                    contentsByCourseIds(courseIds: $courseIds) {
                        id
                        metadata {
                            courseId
                        }
                    }
                }
                """;

        graphQlTester.document(query)
                .variable("courseIds", courseIds.subList(0, 1))
                .execute()
                .path("contentsByCourseIds[*][*].metadata.courseId")
                .entityList(UUID.class)
                .containsExactly(courseIds.get(0));
    }
}
