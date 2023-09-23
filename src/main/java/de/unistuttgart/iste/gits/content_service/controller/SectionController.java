package de.unistuttgart.iste.gits.content_service.controller;

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
    public SectionMutation mutateSection(@Argument final UUID sectionId) {
        //parent object for nested mutations
        return new SectionMutation(sectionId, sectionId);
    }

    @MutationMapping
    public Section createSection(@Argument final CreateSectionInput input) {
        return sectionService.createSection(input);
    }

    @SchemaMapping(typeName = "SectionMutation")
    public Section updateSectionName(@Argument final String name, final SectionMutation sectionMutation) {
        return sectionService.updateSectionName(sectionMutation.getSectionId(), name);
    }

    @SchemaMapping(typeName = "SectionMutation")
    public UUID deleteSection(final SectionMutation sectionMutation) {
        return sectionService.deleteWorkPath(sectionMutation.getSectionId());
    }

    @SchemaMapping(typeName = "SectionMutation")
    public Section updateStageOrder(@Argument final List<UUID> stages, final SectionMutation sectionMutation) {
        return sectionService.reorderStages(sectionMutation.getSectionId(), stages);
    }

    @QueryMapping
    public List<List<Section>> sectionsByChapterIds(@Argument final List<UUID> chapterIds) {
        return sectionService.getSectionsByChapterIds(chapterIds);
    }

}
