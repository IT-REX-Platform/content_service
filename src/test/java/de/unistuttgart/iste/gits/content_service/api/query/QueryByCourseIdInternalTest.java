package de.unistuttgart.iste.gits.content_service.api.query;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.content_service.TestData;
import de.unistuttgart.iste.gits.content_service.persistence.entity.ContentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.repository.ContentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.util.List;
import java.util.UUID;

/**
 * Like {@link QueryByCourseIdTest} but without the {@link de.unistuttgart.iste.gits.common.testutil.InjectCurrentUserHeader}
 * to test the internal variant of the query
 */
@GraphQlApiTest
class QueryByCourseIdInternalTest {

    @Autowired
    private ContentRepository contentRepository;

    /**
     * Test for the internal variant of the query
     */
    @Test
    void testQueryByCourseId(final GraphQlTester graphQlTester) {
        final List<UUID> courseIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

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
                    _internal_noauth_contentsByCourseIds(courseIds: $courseIds) {
                        id
                        metadata {
                            courseId
                        }
                    }
                }
                """;

        graphQlTester.document(query)
                .variable("courseIds", courseIds.subList(0, 3))
                .execute()
                .path("_internal_noauth_contentsByCourseIds[*][*].metadata.courseId")
                .entityList(UUID.class)
                .containsExactly(courseIds.get(0), courseIds.get(1), courseIds.get(2));
    }
}
