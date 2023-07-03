package de.unistuttgart.iste.gits.content_service.controller;

import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.gits.content_service.service.ContentService;
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

    @QueryMapping
    public ContentPayload contents() {
        return contentService.getAllContents();
    }

    @QueryMapping
    public ContentPayload contentsByIds(@Argument List<UUID> ids) {
        return contentService.getContentsById(ids);
    }

    @QueryMapping
    List<List<Content>> contentsByChapterIds(@Argument List<UUID> chapterIds) {
        return contentService.getContentsByChapterIds(chapterIds);
    }

    @MutationMapping
    public MediaContent createMediaContent(@Argument CreateMediaContentInput input) {
        return contentService.createMediaContent(input);
    }

    @MutationMapping
    public MediaContent updateMediaContent(@Argument UpdateMediaContentInput input) {
        return contentService.updateMediaContent(input);
    }

    @MutationMapping
    public Assessment createAssessment(@Argument CreateAssessmentInput input) {
        return contentService.createAssessment(input);
    }

    @MutationMapping
    public Assessment updateAssessment(@Argument UpdateAssessmentInput input) {
        return contentService.updateAssessment(input);
    }

    @MutationMapping
    public UUID deleteContent(@Argument UUID id) {
        return contentService.deleteContent(id);
    }

    @MutationMapping
    public Content addTagToContent(@Argument("contentId") UUID id, @Argument String tagName) {
        return contentService.addTagToContent(id, tagName);
    }

    @MutationMapping
    public Content removeTagFromContent(@Argument("contentId") UUID id, @Argument String tagName) {
        return contentService.removeTagFromContent(id, tagName);
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

    public abstract class AssessmentResolver<T extends Assessment> extends ContentResolver<T> { }

    @Controller
    public class MediaContentResolver extends ContentResolver<MediaContent> { }

    @Controller
    public class FlashcardSetAssessmentResolver extends ContentResolver<FlashcardSetAssessment> { }

}

