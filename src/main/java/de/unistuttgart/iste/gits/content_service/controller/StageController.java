package de.unistuttgart.iste.gits.content_service.controller;

import de.unistuttgart.iste.gits.common.user_handling.LoggedInUser;
import de.unistuttgart.iste.gits.content_service.service.StageService;
import de.unistuttgart.iste.gits.content_service.service.UserProgressDataService;
import de.unistuttgart.iste.gits.generated.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class StageController {

    private final StageService stageService;
    private final UserProgressDataService userProgressDataService;

    @SchemaMapping(typeName = "SectionMutation")
    public Stage createStage(@Argument final CreateStageInput input, final SectionMutation sectionMutation) {
        return stageService.createNewStage(sectionMutation.getSectionId(), input);
    }

    @SchemaMapping(typeName = "SectionMutation")
    public Stage updateStage(@Argument final UpdateStageInput input, final SectionMutation sectionMutation) {
        return stageService.updateStage(input);
    }

    @SchemaMapping(typeName = "SectionMutation")
    public UUID deleteStage(@Argument final UUID id, final SectionMutation sectionMutation) {
        return stageService.deleteStage(id);
    }

    @SchemaMapping(field = "requiredContentsProgress")
    public double requiredContentsProgress(final Stage stage, @ContextValue final LoggedInUser currentUser) {
        return userProgressDataService.getStageProgressForUser(stage, currentUser.getId(), true);
    }

    @SchemaMapping(field = "optionalContentsProgress")
    public double optionalContentsProgress(final Stage stage, @ContextValue final LoggedInUser currentUser) {
        return userProgressDataService.getStageProgressForUser(stage, currentUser.getId(), false);
    }

}
