package de.unistuttgart.iste.gits.content_service.controller;

import de.unistuttgart.iste.gits.content_service.service.SectionService;
import de.unistuttgart.iste.gits.generated.dto.CreateSectionInput;
import de.unistuttgart.iste.gits.generated.dto.Section;
import de.unistuttgart.iste.gits.generated.dto.SectionMutation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SectionController {

    private final SectionService sectionService;

    @MutationMapping
    public SectionMutation mutateSection(@Argument UUID sectionId) {
        //parent object for nested mutations
        return new SectionMutation(sectionId, sectionId);
    }

    @MutationMapping
    public Section createSection(@Argument CreateSectionInput input) {
        return sectionService.createSection(input);
    }

    @SchemaMapping(typeName = "SectionMutation")
    public Section updateSectionName(@Argument String name, SectionMutation sectionMutation) {
        return sectionService.updateSectionName(sectionMutation.getSectionId(), name);
    }

    @SchemaMapping(typeName = "SectionMutation")
    public UUID deleteSection(SectionMutation sectionMutation) {
        return sectionService.deleteWorkPath(sectionMutation.getSectionId());
    }

    @SchemaMapping(typeName = "SectionMutation")
    public Section updateStageOrder(@Argument List<UUID> stages, SectionMutation sectionMutation) {
        return sectionService.reorderStages(sectionMutation.getSectionId(), stages);
    }

    @QueryMapping
    public List<List<Section>> sectionsByChapterIds(@Argument List<UUID> chapterIds) {
        return sectionService.getSectionsByChapterIds(chapterIds);
    }

}
