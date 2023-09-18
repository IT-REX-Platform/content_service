package de.unistuttgart.iste.gits.content_service.persistence.mapper;

import de.unistuttgart.iste.gits.content_service.persistence.entity.*;
import de.unistuttgart.iste.gits.generated.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContentMapper {

    private final ModelMapper modelMapper;

    public Content entityToDto(ContentEntity contentEntity) {
        Content result;
        if (contentEntity.getMetadata().getType() == ContentType.MEDIA) {
            result = mediaContentEntityToDto(contentEntity);
        } else {
            result = assessmentEntityToDto(contentEntity);
        }

        return result;
    }

    public MediaContentEntity mediaContentDtoToEntity(CreateMediaContentInput input) {
        var result = modelMapper.map(input, MediaContentEntity.class);
        result.getMetadata().setTags(tagsFromNames(input.getMetadata().getTagNames()));
        return result;
    }

    public MediaContentEntity mediaContentDtoToEntity(UUID contentId, UpdateMediaContentInput input, ContentType contentType) {
        var result = modelMapper.map(input, MediaContentEntity.class);
        result.getMetadata().setType(contentType);
        result.getMetadata().setTags(tagsFromNames(input.getMetadata().getTagNames()));
        result.setId(contentId);
        return result;
    }

    public MediaContent mediaContentEntityToDto(ContentEntity contentEntity) {
        MediaContent result = modelMapper.map(contentEntity, MediaContent.class);
        result.getMetadata().setTagNames(contentEntity.getTagNames());
        return result;
    }

    public AssessmentEntity assessmentDtoToEntity(CreateAssessmentInput input) {
        var result = modelMapper.map(input, AssessmentEntity.class);
        result.getMetadata().setTags(tagsFromNames(input.getMetadata().getTagNames()));
        return result;
    }

    public AssessmentEntity assessmentDtoToEntity(UUID contentId,
                                                  UpdateAssessmentInput input,
                                                  ContentType contentType) {
        var result = modelMapper.map(input, AssessmentEntity.class);
        result.getMetadata().setType(contentType);
        result.getMetadata().setTags(tagsFromNames(input.getMetadata().getTagNames()));
        result.setId(contentId);
        return result;
    }

    private Set<TagEntity> tagsFromNames(List<String> tagNames) {
        return tagNames.stream().map(TagEntity::fromName).collect(Collectors.toSet());
    }

    public Assessment assessmentEntityToDto(ContentEntity contentEntity) {
        Assessment result;
        if (contentEntity.getMetadata().getType() == ContentType.FLASHCARDS) {
            result = modelMapper.map(contentEntity, FlashcardSetAssessment.class);
        } else if (contentEntity.getMetadata().getType() == ContentType.QUIZ) {
            result = modelMapper.map(contentEntity, QuizAssessment.class);
        } else {
            // put other assessment types here
            throw new IllegalStateException("Unsupported content type for assessment: " + contentEntity.getMetadata().getType());
        }

        result.getMetadata().setTagNames(contentEntity.getTagNames());
        return result;
    }

}
