package de.unistuttgart.iste.gits.content_service.controller;

import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.gits.content_service.service.*;
import de.unistuttgart.iste.gits.generated.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ContentController {

    public static final String INTERNAL_NOAUTH_PREFIX = "_internal_noauth_";
    private static final String CONTENT_MUTATION = "ContentMutation";

    private final ContentService contentService;
    private final UserProgressDataService userProgressDataService;
    private final SuggestionService suggestionService;

    @QueryMapping
    public ContentPayload contents() {
        return contentService.getAllContents();
    }

    @QueryMapping
    public List<Content> contentsByIds(@Argument final List<UUID> ids) {
        return contentService.getContentsById(ids);
    }

    @QueryMapping
    public List<Content> findContentsByIds(@Argument final List<UUID> ids) {
        return contentService.findContentsById(ids);
    }

    @QueryMapping
    public List<Suggestion> suggestionsByChapterIds(@Argument final List<UUID> chapterIds,
                                                    @Argument final int amount,
                                                    @Argument final List<SkillType> skillTypes,
                                                    @ContextValue final LoggedInUser currentUser) {
        return suggestionService.createSuggestions(chapterIds, currentUser.getId(), amount, skillTypes);
    }

    @QueryMapping
    public List<List<Content>> contentsByChapterIds(@Argument final List<UUID> chapterIds) {
        return contentService.getContentsByChapterIds(chapterIds);
    }

    @QueryMapping(name = INTERNAL_NOAUTH_PREFIX + "achievableSkillTypesByChapterIds")
    public List<List<SkillType>> internalAchievableSkillTypesByChapterIds(@Argument final List<UUID> chapterIds) {
        return contentService.getAchievableSkillTypesByChapterIds(chapterIds);
    }

    @MutationMapping
    public ContentMutation mutateContent(@Argument final UUID contentId) {
        //parent object for nested mutations
        return new ContentMutation(contentId, contentId);
    }

    @MutationMapping
    public MediaContent createMediaContent(@Argument final CreateMediaContentInput input) {
        return contentService.createMediaContent(input);
    }

    @MutationMapping
    public Assessment createAssessment(@Argument final CreateAssessmentInput input) {
        return contentService.createAssessment(input);
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

    @QueryMapping
    public List<CompositeProgressInformation> progressByChapterIds(@Argument List<UUID> chapterIds, @ContextValue LoggedInUser currentUser) {
        return userProgressDataService.getProgressByChapterIdsForUser(chapterIds, currentUser.getId());
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

    public abstract class AssessmentResolver<T extends Assessment> extends ContentResolver<T> {
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

}

