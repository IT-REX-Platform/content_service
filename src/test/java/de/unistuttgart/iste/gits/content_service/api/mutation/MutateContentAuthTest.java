package de.unistuttgart.iste.gits.content_service.api.mutation;

import de.unistuttgart.iste.gits.common.testutil.*;
import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser.UserRoleInCourse;
import de.unistuttgart.iste.gits.content_service.TestData;
import de.unistuttgart.iste.gits.content_service.persistence.entity.ContentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.repository.ContentRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.test.annotation.Commit;

import java.util.UUID;

import static de.unistuttgart.iste.gits.common.testutil.TestUsers.userWithMembershipInCourseWithId;

@GraphQlApiTest
class MutateContentAuthTest {

    @Autowired
    private ContentRepository contentRepository;
    private final UUID courseId = UUID.randomUUID();

    @InjectCurrentUserHeader
    private final LoggedInUser loggedInUser = userWithMembershipInCourseWithId(courseId, UserRoleInCourse.TUTOR);

    @Test
    @Transactional
    @Commit
    void testMutateContentWithTutorRole(final GraphQlTester graphQlTester) {
        final ContentEntity content = contentRepository.save(TestData.dummyMediaContentEntityBuilder(courseId).build());
        final UUID contentId = content.getId();

        final String query = """
                mutation ($contentId: UUID!) {
                    mutateContent(contentId: $contentId) {
                        contentId
                    }
                }
                """;

        graphQlTester.document(query)
                .variable("contentId", contentId)
                .execute()
                .errors()
                .satisfy(AuthorizationAsserts::assertIsMissingUserRoleError);
    }
}
