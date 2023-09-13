package de.unistuttgart.iste.gits.content_service.controller;

import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.gits.content_service.service.ContentService;
import de.unistuttgart.iste.gits.content_service.service.SuggestionService;
import de.unistuttgart.iste.gits.content_service.service.UserProgressDataService;
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

    private final ContentService contentService;
    private final UserProgressDataService userProgressDataService;
    private final SuggestionService suggestionService;

    @QueryMapping
    public ContentPayload contents() {
        return contentService.getAllContents();
    }

    @QueryMapping
    public List<Content> contentsByIds(@Argument List<UUID> ids) {
        return contentService.getContentsById(ids);
    }

    @QueryMapping
    public List<Content> findContentsByIds(@Argument List<UUID> ids) {
        return contentService.findContentsById(ids);
    }

    @QueryMapping
    public List<Suggestion> suggestionsByChapterIds(@Argument List<UUID> chapterIds,
                                                    @Argument int amount,
                                                    @Argument List<SkillType> skillTypes,
                                                    @ContextValue LoggedInUser currentUser) {
        return suggestionService.createSuggestions(chapterIds, currentUser.getId(), amount, skillTypes);
    }

    @QueryMapping
    public List<List<Content>> contentsByChapterIds(@Argument List<UUID> chapterIds) {
        return contentService.getContentsByChapterIds(chapterIds);
    }

    @MutationMapping
    public ContentMutation mutateContent(@Argument UUID contentId) {
        //parent object for nested mutations
        return new ContentMutation(contentId, contentId);
    }

    @MutationMapping
    public MediaContent createMediaContent(@Argument CreateMediaContentInput input) {
        return contentService.createMediaContent(input);
    }

    @SchemaMapping(typeName = "ContentMutation")
    public MediaContent updateMediaContent(@Argument UpdateMediaContentInput input, ContentMutation contentMutation) {
        return contentService.updateMediaContent(contentMutation.getContentId(), input);
    }

    @MutationMapping
    public Assessment createAssessment(@Argument CreateAssessmentInput input) {
        return contentService.createAssessment(input);
    }

    @SchemaMapping(typeName = "ContentMutation")
    public Assessment updateAssessment(@Argument UpdateAssessmentInput input, ContentMutation contentMutation) {
        return contentService.updateAssessment(contentMutation.getContentId(), input);
    }

    @SchemaMapping(typeName = "ContentMutation")
    public UUID deleteContent(ContentMutation contentMutation) {
        return contentService.deleteContent(contentMutation.getContentId());
    }

    @SchemaMapping(typeName = "ContentMutation")
    public Content addTagToContent(@Argument String tagName, ContentMutation contentMutation) {
        return contentService.addTagToContent(contentMutation.getContentId(), tagName);
    }

    @SchemaMapping(typeName = "ContentMutation")
    public Content removeTagFromContent(@Argument String tagName, ContentMutation contentMutation) {
        return contentService.removeTagFromContent(contentMutation.getContentId(), tagName);
    }

    public abstract class ContentResolver<T extends Content> {
        @SchemaMapping(field = "userProgressData")
        public UserProgressData userProgressData(T content, @ContextValue LoggedInUser currentUser) {
            return userProgressDataService.getUserProgressData(currentUser.getId(), content.getId());
        }

        @SchemaMapping(field = "progressDataForUser")
        public UserProgressData progressDataForUser(T content, @Argument UUID userId) {
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

