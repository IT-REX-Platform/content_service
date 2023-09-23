package de.unistuttgart.iste.gits.content_service.persistence.mapper;

import de.unistuttgart.iste.gits.content_service.persistence.entity.StageEntity;
import de.unistuttgart.iste.gits.generated.dto.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class StageMapper {

    private final ContentMapper contentMapper;

    public Stage entityToDto(final StageEntity entity) {
        return Stage.builder()
                .setId(entity.getId())
                .setPosition(entity.getPosition())
                .setOptionalContents(entity.getOptionalContents()
                        .stream()
                        .map(contentMapper::entityToDto)
                        .toList()
                )
                .setRequiredContents(entity.getRequiredContents()
                        .stream()
                        .map(contentMapper::entityToDto)
                        .toList()
                )
                .build();
    }
}
