package de.unistuttgart.iste.gits.content_service.api.query;

import de.unistuttgart.iste.gits.common.testutil.GitsPostgresSqlContainer;
import de.unistuttgart.iste.gits.common.testutil.GraphQlApiTest;
import de.unistuttgart.iste.gits.content_service.TestData;
import de.unistuttgart.iste.gits.content_service.persistence.dao.ContentEntity;
import de.unistuttgart.iste.gits.content_service.persistence.repository.ContentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.testcontainers.junit.jupiter.Container;

import java.util.List;
import java.util.UUID;

@GraphQlApiTest
class QueryByChapterIdTest {
    @Container
    static final GitsPostgresSqlContainer postgres = GitsPostgresSqlContainer.getInstance();

    @Autowired
    private ContentRepository contentRepository;

    /**
     * Given valid chapterIds
     * When the queryByChapterId query is called
     * Then the content is returned, correctly grouped and filtered by chapterId
     */
    @Test
    void testQueryByChapterId(GraphQlTester graphQlTester) {
        List<UUID> chapterIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        chapterIds.stream()
                .<ContentEntity>map(chapterId -> (
                        TestData.dummyMediaContentEntityBuilder()
                                .metadata(TestData.dummyContentMetadataEmbeddableBuilder()
                                        .chapterId(chapterId)
                                        .build()))
                        .build())
                .forEach(contentRepository::save);

        String query = """
                query($chapterIds: [UUID!]!) {
                    contentsByChapterIds(chapterIds: $chapterIds) {
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
                .path("contentsByChapterIds[*][*].metadata.chapterId")
                .entityList(UUID.class)
                .containsExactly(chapterIds.get(0), chapterIds.get(1), chapterIds.get(2));
    }
}
