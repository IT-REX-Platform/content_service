package de.unistuttgart.iste.gits.content_service.controller;

import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.gits.common.user_handling.UserCourseAccessValidator;
import de.unistuttgart.iste.gits.content_service.service.SectionService;
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
public class SectionController {

    private final SectionService sectionService;

    @MutationMapping
    public SectionMutation mutateSection(@Argument final UUID sectionId,
                                         @ContextValue final LoggedInUser currentUser) {
        Section section = sectionService.getSectionById(sectionId);

        // check if the user is admin in the course, otherwise throw an exception
        UserCourseAccessValidator.validateUserHasAccessToCourse(currentUser,
                LoggedInUser.UserRoleInCourse.ADMINISTRATOR,
                section.getCourseId());

        //parent object for nested mutations
        return new SectionMutation(sectionId, sectionId);
    }

    @MutationMapping(name = "_internal_createSection")
    public Section createSection(@Argument final CreateSectionInput input,
                                 @Argument final UUID courseId,
                                 @ContextValue final LoggedInUser currentUser) {
        UserCourseAccessValidator.validateUserHasAccessToCourse(currentUser,
                LoggedInUser.UserRoleInCourse.ADMINISTRATOR,
                courseId);

        return sectionService.createSection(courseId, input);
    }

    @SchemaMapping(typeName = "SectionMutation")
    public Section updateSectionName(@Argument final String name, final SectionMutation sectionMutation) {
        return sectionService.updateSectionName(sectionMutation.getSectionId(), name);
    }

    @SchemaMapping(typeName = "SectionMutation")
    public UUID deleteSection(final SectionMutation sectionMutation) {
        return sectionService.deleteSection(sectionMutation.getSectionId());
    }

    @SchemaMapping(typeName = "SectionMutation")
    public Section updateStageOrder(@Argument final List<UUID> stages, final SectionMutation sectionMutation) {
        return sectionService.reorderStages(sectionMutation.getSectionId(), stages);
    }

    @QueryMapping(name = "_internal_noauth_sectionsByChapterIds")
    public List<List<Section>> internalNoauthSectionsByChapterIds(@Argument final List<UUID> chapterIds) {
        return sectionService.getSectionsByChapterIds(chapterIds);
    }

}
