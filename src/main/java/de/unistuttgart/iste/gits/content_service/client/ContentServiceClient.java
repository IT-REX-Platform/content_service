package de.unistuttgart.iste.gits.content_service.client;

import de.unistuttgart.iste.gits.content_service.exception.ContentServiceConnectionException;
import de.unistuttgart.iste.gits.generated.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.springframework.graphql.client.ClientGraphQlResponse;
import org.springframework.graphql.client.GraphQlClient;
import reactor.core.publisher.SynchronousSink;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * Client for the content service, allowing to query contents with user progress data.
 */
@Slf4j
public class ContentServiceClient {

    private static final long RETRY_COUNT = 3;
    private final GraphQlClient graphQlClient;
    private final ModelMapper modelMapper;

    public ContentServiceClient(final GraphQlClient graphQlClient) {
        this.graphQlClient = graphQlClient;
        this.modelMapper = new ModelMapper();
        modelMapper.typeMap(String.class, OffsetDateTime.class).setConverter(stringToOffsetDateTimeConverter());
    }

    /**
     * Queries the content service for all contents of the given chapter.
     *
     * @param userId    the id of the user for which to query the progress data
     * @param chapterId the id of the chapter for which to query the contents
     * @return a list of contents of the given chapter
     * @throws ContentServiceConnectionException if the connection to the content
     *                                           service fails or any other error occurs
     */
    public List<Content> queryContentsOfChapter(final UUID userId, final UUID chapterId) throws ContentServiceConnectionException {
        log.info("Querying contents of chapter with id {}", chapterId);
        log.info("Query", QueryDefinitions.CONTENTS_BY_CHAPTER_IDS_QUERY);

        try {
            return graphQlClient.document(QueryDefinitions.CONTENTS_BY_CHAPTER_IDS_QUERY)
                    .variable("chapterIds", List.of(chapterId))
                    .variable("userId", userId)
                    .execute()
                    .handle((ClientGraphQlResponse result, SynchronousSink<List<Content>> sink)
                            -> handleContentServiceResponse(result, sink, QueryDefinitions.CONTENTS_BY_CHAPTER_ID_QUERY_NAME))
                    .retry(RETRY_COUNT)
                    .block();
        } catch (final RuntimeException e) {
            unwrapContentServiceConnectionException(e);
        }
        return List.of(); // unreachable
    }

    /**
     * Queries the content service for all contents of the given course.
     *
     * @param userId   the id of the user for which to query the progress data
     * @param courseId the id of the course for which to query the contents
     * @return a list of contents of the given course
     * @throws ContentServiceConnectionException if the connection to the content
     *                                           service fails or any other error occurs
     */
    public List<Content> queryContentsOfCourse(final UUID userId, final UUID courseId) throws ContentServiceConnectionException {
        log.info("Querying contents of course with id {}", courseId);

        try {
            return graphQlClient.document(QueryDefinitions.CONTENTS_BY_COURSE_IDS_QUERY)
                    .variable("courseIds", List.of(courseId))
                    .variable("userId", userId)
                    .execute()
                    .handle((ClientGraphQlResponse result, SynchronousSink<List<Content>> sink)
                            -> handleContentServiceResponse(result, sink, QueryDefinitions.CONTENTS_BY_COURSE_ID_QUERY_NAME))
                    .retry(RETRY_COUNT)
                    .block();
        } catch (final RuntimeException e) {
            unwrapContentServiceConnectionException(e);
        }
        return List.of(); // unreachable
    }

    private static void unwrapContentServiceConnectionException(final RuntimeException e) throws ContentServiceConnectionException {
        // block wraps exceptions in a RuntimeException, so we need to unwrap them
        if (e.getCause() instanceof final ContentServiceConnectionException contentServiceConnectionException) {
            throw contentServiceConnectionException;
        }
        // if the exception is not a ContentServiceConnectionException, we don't know how to handle it
        throw e;
    }

    private void handleContentServiceResponse(final ClientGraphQlResponse result,
                                              final SynchronousSink<List<Content>> sink,
                                              final String queryName) {
        log.info(result.toString());
        if (!result.isValid()) {
            sink.error(new ContentServiceConnectionException(
                    "Error while fetching contents from content service: Invalid response.",
                    result.getErrors()));
            return;
        }

        final List<Content> retrievedContents;
        try {
            retrievedContents = convertResponseToListOfContent(result, queryName);
        } catch (final ContentServiceConnectionException e) {
            sink.error(e);
            return;
        }

        sink.next(retrievedContents);
    }

    private List<Content> convertResponseToListOfContent(final ClientGraphQlResponse result,
                                                         final String queryName)
            throws ContentServiceConnectionException {

        final List<Map<String, Object>> contentFields = result.field(queryName + "[0]").getValue();

        if (contentFields == null) {
            throw new ContentServiceConnectionException(
                    "Error while fetching contents from content service: Missing field in response.");
        }

        return createContentObjects(contentFields);
    }

    private List<Content> createContentObjects(final List<Map<String, Object>> contentFields) {
        final List<Content> retrievedContents = new ArrayList<>(contentFields.size());

        for (final Map<String, Object> contentField : contentFields) {
            final ContentMetadata metadata = getMetadata(contentField);
            final UUID id = getId(contentField);
            final UserProgressData progressDataForUser = getProgressDataForUser(contentField);

            AssessmentMetadata assessmentMetadata = null;
            if (contentField.containsKey("assessmentMetadata")) {
                assessmentMetadata = getAssessmentMetadata(contentField);
            }
            retrievedContents.add(convertToCorrespondingContent(metadata, assessmentMetadata, id, progressDataForUser));
        }
        return retrievedContents;
    }

    private AssessmentMetadata getAssessmentMetadata(final Map<String, Object> contentField) {
        return modelMapper.map(contentField.get("assessmentMetadata"), AssessmentMetadata.class);
    }

    private UserProgressData getProgressDataForUser(final Map<String, Object> contentField) {
        return modelMapper.map(contentField.get("progressDataForUser"), UserProgressData.class);
    }

    private UUID getId(final Map<String, Object> contentField) {
        return UUID.fromString((String) contentField.get("id"));
    }

    private ContentMetadata getMetadata(final Map<String, Object> contentField) {
        return modelMapper.map(contentField.get("metadata"), ContentMetadata.class);
    }

    private Content convertToCorrespondingContent(final ContentMetadata metadata,
                                                  final AssessmentMetadata assessmentMetadata,
                                                  final UUID id,
                                                  final UserProgressData progressDataForUser) {
        switch (metadata.getType()) {
            case FLASHCARDS -> {
                return new FlashcardSetAssessment(assessmentMetadata, id, metadata, progressDataForUser);
            }
            case QUIZ -> {
                return new QuizAssessment(assessmentMetadata, id, metadata, progressDataForUser);
            }
            case MEDIA -> {
                return new MediaContent(id, metadata, progressDataForUser);
            }
            default -> throw new IllegalArgumentException("Unknown assessment type: " + metadata.getType());
        }
    }

    private Converter<String, OffsetDateTime> stringToOffsetDateTimeConverter() {
        return context -> {
            final String source = context.getSource();
            if (source == null) {
                return null;
            }
            return OffsetDateTime.parse(source);
        };
    }

}
