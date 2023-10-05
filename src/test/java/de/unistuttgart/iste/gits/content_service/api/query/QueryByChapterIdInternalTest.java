package de.unistuttgart.iste.gits.content_service.api.query;

import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.common.testutil.InjectCurrentUserHeader;
import de.unistuttgart.iste.gits.content_service.TestData;
import de.unistuttgart.iste.gits.content_service.persistence.repository.ContentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.util.List;
import java.util.UUID;

/**
 * Like {@link QueryByCourseIdTest} but without the {@link InjectCurrentUserHeader} to test the internal variant of the query
 */
@GraphQlApiTest
class QueryByChapterIdInternalTest {

    @Autowired
    private ContentRepository contentRepository;

    /**
     * Test for the internal variant of the query 
     */
    @Test
    void testQueryByChapterId(final GraphQlTester graphQlTester) {
        final UUID courseId = UUID.randomUUID();
        final List<UUID> chapterIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        chapterIds.stream()
                .map(chapterId -> (
                        TestData.dummyMediaContentEntityBuilder(courseId)
                                .metadata(TestData.dummyContentMetadataEmbeddableBuilder(courseId)
                                        .chapterId(chapterId)
                                        .build()))
                        .build())
                .forEach(contentRepository::save);

        final String query = """
                query($chapterIds: [UUID!]!) {
                    _internal_noauth_contentsByChapterIds(chapterIds: $chapterIds) {
                        id
                        metadata {
                            chapterId
                        }
                    }
                }
                """;

        graphQlTester.document(query)
                .variable("chapterIds", chapterIds.subList(0, 3))
                .execute()
                .path("_internal_noauth_contentsByChapterIds[*][*].metadata.chapterId")
                .entityList(UUID.class)
                .containsExactly(chapterIds.get(0), chapterIds.get(1), chapterIds.get(2));
    }
}
