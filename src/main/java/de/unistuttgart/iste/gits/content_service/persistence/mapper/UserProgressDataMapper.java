package de.unistuttgart.iste.gits.content_service.persistence.mapper;

import de.unistuttgart.iste.gits.common.event.UserProgressLogEvent;
import de.unistuttgart.iste.gits.content_service.persistence.dao.ProgressLogItemEmbeddable;
import de.unistuttgart.iste.gits.content_service.persistence.dao.UserProgressDataEntity;
import de.unistuttgart.iste.gits.generated.dto.UserProgressData;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserProgressDataMapper {

    private final ModelMapper modelMapper;

    public UserProgressData entityToDto(UserProgressDataEntity userProgressDataEntity) {
        return modelMapper.map(userProgressDataEntity, UserProgressData.class);
    }

    public ProgressLogItemEmbeddable eventToEmbeddable(UserProgressLogEvent userProgressLogEvent) {
        return modelMapper.map(userProgressLogEvent, ProgressLogItemEmbeddable.class);
    }
}
