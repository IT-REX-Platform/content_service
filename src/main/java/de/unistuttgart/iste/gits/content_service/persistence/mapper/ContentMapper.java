package de.unistuttgart.iste.gits.content_service.persistence.mapper;

import de.unistuttgart.iste.gits.content_service.persistence.entity.*;
import de.unistuttgart.iste.gits.generated.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContentMapper {

    private final ModelMapper modelMapper;

    public Content entityToDto(final ContentEntity contentEntity) {
        final Content result;
        if (contentEntity.getMetadata().getType() == ContentType.MEDIA) {
            result = mediaContentEntityToDto(contentEntity);
        } else {
            result = assessmentEntityToDto(contentEntity);
        }

        return result;
    }

    public MediaContentEntity mediaContentDtoToEntity(final CreateMediaContentInput input) {
        final var result = modelMapper.map(input, MediaContentEntity.class);
        result.getMetadata().setTags(new HashSet<>(input.getMetadata().getTagNames()));
        return result;
    }

    public MediaContentEntity mediaContentDtoToEntity(final UUID contentId, final UpdateMediaContentInput input, final ContentType contentType) {
        final var result = modelMapper.map(input, MediaContentEntity.class);
        result.getMetadata().setType(contentType);
        result.getMetadata().setTags(new HashSet<>(input.getMetadata().getTagNames()));
        result.setId(contentId);
        return result;
    }

    public MediaContent mediaContentEntityToDto(final ContentEntity contentEntity) {
        final MediaContent result = modelMapper.map(contentEntity, MediaContent.class);
        result.getMetadata().setTagNames(new ArrayList<>(contentEntity.getMetadata().getTags()));
        return result;
    }

    public AssessmentEntity assessmentDtoToEntity(final CreateAssessmentInput input) {
        final var result = modelMapper.map(input, AssessmentEntity.class);
        result.getMetadata().setTags(new HashSet<>(input.getMetadata().getTagNames()));
        return result;
    }

    public AssessmentEntity assessmentDtoToEntity(final UUID contentId,
                                                  final UpdateAssessmentInput input,
                                                  final ContentType contentType) {
        final var result = modelMapper.map(input, AssessmentEntity.class);
        result.getMetadata().setType(contentType);
        result.getMetadata().setTags(new HashSet<>(input.getMetadata().getTagNames()));
        result.setId(contentId);
        return result;
    }

    public Assessment assessmentEntityToDto(final ContentEntity contentEntity) {
        final Assessment result;
        if (contentEntity.getMetadata().getType() == ContentType.FLASHCARDS) {
            result = modelMapper.map(contentEntity, FlashcardSetAssessment.class);
        } else if (contentEntity.getMetadata().getType() == ContentType.QUIZ) {
            result = modelMapper.map(contentEntity, QuizAssessment.class);
        } else {
            // put other assessment types here
            throw new IllegalStateException("Unsupported content type for assessment: " + contentEntity.getMetadata().getType());
        }

        result.getMetadata().setTagNames(new ArrayList<>(contentEntity.getMetadata().getTags()));
        return result;
    }

}
