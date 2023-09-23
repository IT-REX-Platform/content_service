package de.unistuttgart.iste.gits.content_service.persistence.mapper;

import de.unistuttgart.iste.gits.content_service.persistence.entity.SectionEntity;
import de.unistuttgart.iste.gits.generated.dto.Section;
import de.unistuttgart.iste.gits.generated.dto.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
@RequiredArgsConstructor
public class SectionMapper {

    private final StageMapper stageMapper;

    public Section entityToDto(final SectionEntity entity) {

        return Section.builder()
                .setId(entity.getId())
                .setChapterId(entity.getChapterId())
                .setName(entity.getName())
                .setStages(
                        entity.getStages()
                                .stream()
                                .map(stageMapper::entityToDto)
                                .sorted(Comparator.comparingInt(Stage::getPosition))
                                .toList()
                )
                .build();
    }

}
