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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.hasSize;

@GraphQlApiTest
class QueryByIdTest {

    @Autowired
    private ContentRepository contentRepository;

    /**
     * Given valid ids
     * When the contentsByIds query is called
     * Then the contents are returned in the same order as the ids
     */
    @Test
    void getByIdOrderIsConsistent(final GraphQlTester graphQlTester) {
        final List<ContentEntity> contentEntities = List.of(
                contentRepository.save(TestData.dummyMediaContentEntityBuilder().build()),
                contentRepository.save(TestData.dummyMediaContentEntityBuilder().build()),
                contentRepository.save(TestData.dummyMediaContentEntityBuilder().build())
        );

        final List<UUID> ids = contentEntities.stream()
                .map(ContentEntity::getId)
                .toList();

        final String query = """
                query($ids: [UUID!]!) {
                    contentsByIds(ids: $ids) {
                        id
                    }
                }
                """;

        graphQlTester.document(query)
                .variable("ids", ids)
                .execute()
                .path("contentsByIds[*].id")
                .entityList(UUID.class)
                .containsExactly(ids.toArray(UUID[]::new));

        // test the order is correct
        final List<UUID> idsReordered = List.of(ids.get(2), ids.get(0), ids.get(1));

        graphQlTester.document(query)
                .variable("ids", idsReordered)
                .execute()
                .path("contentsByIds[*].id")
                .entityList(UUID.class)
                .containsExactly(idsReordered.toArray(UUID[]::new));
    }

    /**
     * Given valid ids
     * When the findContentsByIds query is called
     * Then the contents are returned in the same order as the ids
     */
    @Test
    void findByIdOrderIsConsistent(final GraphQlTester graphQlTester) {
        final List<ContentEntity> contentEntities = List.of(
                contentRepository.save(TestData.dummyMediaContentEntityBuilder().build()),
                contentRepository.save(TestData.dummyMediaContentEntityBuilder().build()),
                contentRepository.save(TestData.dummyMediaContentEntityBuilder().build())
        );

        final List<UUID> ids = contentEntities.stream()
                .map(ContentEntity::getId)
                .toList();

        final String query = """
                query($ids: [UUID!]!) {
                    findContentsByIds(ids: $ids) {
                        id
                    }
                }
                """;

        graphQlTester.document(query)
                .variable("ids", ids)
                .execute()
                .path("findContentsByIds[*].id")
                .entityList(UUID.class)
                .containsExactly(ids.toArray(UUID[]::new));

        // test the order is correct
        final List<UUID> idsReordered = List.of(ids.get(2), ids.get(0), ids.get(1));

        graphQlTester.document(query)
                .variable("ids", idsReordered)
                .execute()
                .path("findContentsByIds[*].id")
                .entityList(UUID.class)
                .containsExactly(idsReordered.toArray(UUID[]::new));
    }

    /**
     * Given some ids of non-existing contents
     * When the contentsByIds query is called
     * Then an error is returned containing the ids of the non-existing contents
     */
    @Test
    void getByNonExistingIds(final GraphQlTester graphQlTester) {
        final ContentEntity existingContentEntity = contentRepository.save(TestData.dummyMediaContentEntityBuilder().build());

        final List<UUID> ids = List.of(
                existingContentEntity.getId(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        final String query = """
                query($ids: [UUID!]!) {
                    contentsByIds(ids: $ids) {
                        id
                    }
                }
                """;

        graphQlTester.document(query)
                .variable("ids", ids)
                .execute()
                .errors()
                .satisfy(errors -> {
                    assertThat(errors, hasSize(1));
                    final String errorMessage = errors.get(0).getMessage();
                    assertThat(errorMessage, containsStringIgnoringCase("Contents with ids"));
                    assertThat(errorMessage, containsStringIgnoringCase(ids.get(1).toString()));
                    assertThat(errorMessage, containsStringIgnoringCase(ids.get(2).toString()));
                    assertThat(errorMessage, containsStringIgnoringCase("not found"));
                });
    }

    /**
     * Given some ids of non-existing contents
     * When the findContentsByIds query is called
     * Then the contents are returned in the same order as the ids and null is used for the non-existing contents
     */
    @Test
    void findByNonExistingIds(final GraphQlTester graphQlTester) {
        final ContentEntity existingContentEntity = contentRepository.save(TestData.dummyMediaContentEntityBuilder().build());

        final List<UUID> ids = List.of(
                existingContentEntity.getId(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        final String query = """
                query($ids: [UUID!]!) {
                    findContentsByIds(ids: $ids) {
                        id
                    }
                }
                """;

        graphQlTester.document(query)
                .variable("ids", ids)
                .execute()
                .path("findContentsByIds")
                .entityList(ContentEntity.class).containsExactly(
                        ContentEntity.builder().id(existingContentEntity.getId()).build(),
                        null,
                        null
                );
    }

}
