package de.unistuttgart.iste.gits.content_service.controller;

import de.unistuttgart.iste.gits.common.exception.NoAccessToCourseException;
import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser.UserRoleInCourse;
import de.unistuttgart.iste.gits.content_service.service.*;
import de.unistuttgart.iste.gits.generated.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static de.unistuttgart.iste.gits.common.user_handling.UserCourseAccessValidator.validateUserHasAccessToCourse;
import static de.unistuttgart.iste.gits.common.user_handling.UserCourseAccessValidator.validateUserHasAccessToCourses;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ContentController {

    public static final String INTERNAL_NOAUTH_PREFIX = "_internal_noauth_";
    public static final String INTERNAL_PREFIX = "_internal_";
    private static final String CONTENT_MUTATION = "ContentMutation";

    private final ContentService contentService;
    private final UserProgressDataService userProgressDataService;
    private final SuggestionService suggestionService;

    @QueryMapping
    public List<Content> contentsByIds(@Argument final List<UUID> ids,
                                       @ContextValue final LoggedInUser currentUser) {
        final List<Content> contents = contentService.getContentsById(ids);
        final Stream<UUID> courseIds = contents.stream().map(content -> content.getMetadata().getCourseId());
        validateUserHasAccessToCourses(currentUser, UserRoleInCourse.STUDENT, courseIds);

        return contents;
    }

    @QueryMapping
    public List<List<Content>> contentsByCourseIds(@Argument final List<UUID> courseIds,
                                                   @ContextValue final LoggedInUser currentUser) {
        validateUserHasAccessToCourses(currentUser, UserRoleInCourse.STUDENT, courseIds);

        return contentService.getContentsByCourseIds(courseIds);
    }

    @QueryMapping(name = INTERNAL_NOAUTH_PREFIX + "contentsByCourseIds")
    public List<List<Content>> internalNoAuthContentsByCourseIds(@Argument final List<UUID> courseIds) {
        return contentService.getContentsByCourseIds(courseIds);
    }

    @QueryMapping
    public List<Content> findContentsByIds(@Argument final List<UUID> ids,
                                           @ContextValue final LoggedInUser currentUser) {
        return contentService.findContentsById(ids).stream()
                .map(content -> {
                    try {
                        if (content == null) {
                            return null;
                        }
                        // check if the user has access to the course, otherwise return null
                        validateUserHasAccessToCourse(currentUser, UserRoleInCourse.STUDENT, content.getMetadata().getCourseId());
                        return content;
                    } catch (final NoAccessToCourseException ex) {
                        return null;
                    }
                }).toList();
    }

    @QueryMapping
    public List<Suggestion> suggestionsByChapterIds(@Argument final List<UUID> chapterIds,
                                                    @Argument final int amount,
                                                    @Argument final List<SkillType> skillTypes,
                                                    @ContextValue final LoggedInUser currentUser) {
        final List<Content> requiredContents = suggestionService.getAvailableRequiredContentsOfChaptersForUser(
                chapterIds,
                currentUser.getId()
        );

        final List<Content> optionalContents = suggestionService.getAvailableOptionalContentsOfChaptersForUser(
                chapterIds,
                currentUser.getId()
        );

        validateReadAccessToContents(currentUser, requiredContents);
        validateReadAccessToContents(currentUser, optionalContents);

        return suggestionService.createSuggestions(
                requiredContents,
                optionalContents,
                currentUser.getId(),
                amount,
                skillTypes);
    }

    @QueryMapping(name = INTERNAL_NOAUTH_PREFIX + "contentsByChapterIds")
    public List<List<Content>> internalNoAuthContentsByChapterIds(@Argument final List<UUID> chapterIds) {
        return contentService.getContentsByChapterIds(chapterIds);
    }

    @QueryMapping
    public List<List<Content>> contentsByChapterIds(@Argument final List<UUID> chapterIds,
                                                    @ContextValue final LoggedInUser currentUser) {
        final List<List<Content>> contents = contentService.getContentsByChapterIds(chapterIds);

        for (final List<Content> contentsOfChapter : contents) {
            validateReadAccessToContents(currentUser, contentsOfChapter);
        }

        return contents;
    }

    @QueryMapping(name = INTERNAL_NOAUTH_PREFIX + "achievableSkillTypesByChapterIds")
    public List<List<SkillType>> internalAchievableSkillTypesByChapterIds(@Argument final List<UUID> chapterIds) {
        return contentService.getAchievableSkillTypesByChapterIds(chapterIds);
    }

    @MutationMapping
    public ContentMutation mutateContent(@Argument final UUID contentId, @ContextValue final LoggedInUser currentUser) {
        final Content content = contentService.getContentsById(List.of(contentId)).get(0);

        // check if the user is admin in the course, otherwise throw an exception
        validateUserHasAccessToCourse(currentUser, UserRoleInCourse.ADMINISTRATOR, content.getMetadata().getCourseId());

        //parent object for nested mutations
        return new ContentMutation(contentId, contentId);
    }

    @MutationMapping(name = INTERNAL_PREFIX + "createMediaContent")
    public MediaContent internalCreateMediaContent(@Argument final CreateMediaContentInput input,
                                                   @Argument final UUID courseId,
                                                   @ContextValue final LoggedInUser currentUser) {
        validateUserHasAccessToCourse(currentUser, UserRoleInCourse.ADMINISTRATOR, courseId);

        return contentService.createMediaContent(input, courseId);
    }

    @MutationMapping(name = INTERNAL_PREFIX + "createAssessment")
    public Assessment internalCreateAssessment(@Argument final CreateAssessmentInput input,
                                               @Argument final UUID courseId,
                                               @ContextValue final LoggedInUser currentUser) {
        validateUserHasAccessToCourse(currentUser, UserRoleInCourse.ADMINISTRATOR, courseId);

        return contentService.createAssessment(input, courseId);
    }

    @SchemaMapping(typeName = CONTENT_MUTATION)
    public MediaContent updateMediaContent(@Argument final UpdateMediaContentInput input, final ContentMutation contentMutation) {
        return contentService.updateMediaContent(contentMutation.getContentId(), input);
    }

    @SchemaMapping(typeName = CONTENT_MUTATION)
    public Assessment updateAssessment(@Argument final UpdateAssessmentInput input, final ContentMutation contentMutation) {
        return contentService.updateAssessment(contentMutation.getContentId(), input);
    }

    @SchemaMapping(typeName = CONTENT_MUTATION)
    public UUID deleteContent(final ContentMutation contentMutation) {
        return contentService.deleteContent(contentMutation.getContentId());
    }

    @SchemaMapping(typeName = CONTENT_MUTATION)
    public Content addTagToContent(@Argument final String tagName, final ContentMutation contentMutation) {
        return contentService.addTagToContent(contentMutation.getContentId(), tagName);
    }

    @SchemaMapping(typeName = CONTENT_MUTATION)
    public Content removeTagFromContent(@Argument final String tagName, final ContentMutation contentMutation) {
        return contentService.removeTagFromContent(contentMutation.getContentId(), tagName);
    }

    @QueryMapping(name = INTERNAL_NOAUTH_PREFIX + "progressByChapterIds")
    public List<CompositeProgressInformation> internalProgressByChapterIds(@Argument final List<UUID> chapterIds,
                                                                   @ContextValue final LoggedInUser currentUser) {
        return userProgressDataService.getProgressByChapterIdsForUser(chapterIds, currentUser.getId());
    }


    @QueryMapping(name = INTERNAL_NOAUTH_PREFIX + "contentWithNoSectionByChapterIds")
    public List<List<Content>> contentWithNoSectionByChapterIds(@Argument final List<UUID> chapterIds) {
        return contentService.getContentWithNoSection(chapterIds);
    }

    /**
     * Abstract Resolver for all Content Types to avoid code duplication
     */
    public abstract class ContentResolver<T extends Content> {
        @SchemaMapping(field = "userProgressData")
        public UserProgressData userProgressData(final T content, @ContextValue final LoggedInUser currentUser) {
            return userProgressDataService.getUserProgressData(currentUser.getId(), content.getId());
        }

        @SchemaMapping(field = "progressDataForUser")
        public UserProgressData progressDataForUser(final T content, @Argument final UUID userId) {
            return userProgressDataService.getUserProgressData(userId, content.getId());
        }
    }

    @Controller
    public class MediaContentResolver extends ContentResolver<MediaContent> {
    }

    @Controller
    public class FlashcardSetAssessmentResolver extends ContentResolver<FlashcardSetAssessment> {
    }

    @Controller
    public class QuizAssessmentResolver extends ContentResolver<QuizAssessment> {
    }

    private void validateReadAccessToContents(final LoggedInUser currentUser, final List<Content> contents) {
        validateUserHasAccessToCourses(currentUser, UserRoleInCourse.STUDENT,
                contents.stream().map(content -> content.getMetadata().getCourseId()));
    }

}

