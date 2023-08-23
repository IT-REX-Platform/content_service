package de.unistuttgart.iste.gits.content_service.service;

import de.unistuttgart.iste.gits.generated.dto.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Stream;

import static java.time.OffsetDateTime.now;
import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;
import static org.apache.commons.lang3.ObjectUtils.isEmpty;

/**
 * Service for creating suggestions for the user.
 */
@Service
@RequiredArgsConstructor
public class SuggestionService {

    private final SectionService sectionService;
    private final UserProgressDataService userProgressDataService;

    private final Map<Pair<UUID, UUID>, UserProgressData> userProgressDataCache = new HashMap<>();

    /**
     * Creates {@link Suggestion}s for the given chapter IDs and user ID. The suggestions are created based on the
     * user's progress and the given skill types.
     * <p>
     * The suggested contents are prioritized as follows:
     * <ol>
     *     <li>Required contents are always suggested before optional contents.</li>
     *     <li>How many days are left until the suggested date or the next learn date (depending on whether the user
     *     has already learned the content) is considered. The content with the lowest number is suggested first,
     *     which might be negative if the content is already overdue.</li>
     *     <li>New contents are suggested before repetitions.</li>
     *     <li>Contents with more reward points are suggested before contents with less reward points.</li>
     * </ol>
     *
     * @param chapterIds the IDs of the chapters for which suggestions should be created.
     * @param userId     the ID of the user for which suggestions should be created.
     * @param amount     the amount of suggestions to create.
     * @param skillTypes the skill types of the suggestions to create. If the list is empty, all skill types are
     *                   considered. If the list is not empty, only the given skill types are considered and
     *                   media contents are ignored.
     * @return the created suggestions.
     */
    public List<Suggestion> createSuggestions(List<UUID> chapterIds, UUID userId, int amount, List<SkillType> skillTypes) {
        userProgressDataCache.clear();

        List<Stage> availableStages = sectionService.getSectionsByChapterIds(chapterIds)
                .stream()
                .flatMap(Collection::stream)
                .flatMap(section -> getAvailableStagesOfSection(section, userId).stream())
                .toList();

        Stream<Content> requiredContents = availableStages.stream().flatMap(stage -> stage.getRequiredContents().stream());
        Stream<Content> optionalContents = availableStages.stream().flatMap(stage -> stage.getOptionalContents().stream());

        return Stream.concat(
                        filterAndSort(requiredContents, userId, skillTypes),
                        filterAndSort(optionalContents, userId, skillTypes))
                .limit(amount)
                .map(content -> createSuggestion(content, getUserProgressData(userId, content.getId())))
                .toList();
    }

    /**
     * Filters the given contents by the given skill types and sorts them according to the prioritization described
     * in {@link SuggestionService#createSuggestions(List, UUID, int, List)}.
     */
    private Stream<Content> filterAndSort(Stream<Content> contents, UUID userId, List<SkillType> skillTypes) {
        return contents
                .filter(content -> hasCorrectSkillType(content, skillTypes))
                // sort by due date for new contents and next learn date for repetitions
                .sorted(comparing((Content content) -> Duration.between(now(), getRelevantLearnDate(content, userId)).toDays())
                        .thenComparing(content -> getUserProgressData(userId, content.getId()).getIsLearned())
                        .thenComparing(content -> content.getMetadata().getRewardPoints(), reverseOrder()));
    }

    /**
     * @return the date when the user should learn the given content next, which is either the suggested date or the
     * next learn date, depending on whether the user has already learned the content.
     */
    private OffsetDateTime getRelevantLearnDate(Content content, UUID userId) {
        UserProgressData userProgressData = getUserProgressData(userId, content.getId());

        if (userProgressData.getIsLearned()) {
            return userProgressData.getNextLearnDate();
        }

        return content.getMetadata().getSuggestedDate();
    }

    private Suggestion createSuggestion(Content content, UserProgressData userProgressData) {
        SuggestionType type = userProgressData.getIsLearned()
                ? SuggestionType.REPETITION
                : SuggestionType.NEW_CONTENT;

        return new Suggestion(content, type);
    }

    private List<Stage> getAvailableStagesOfSection(Section section, UUID userId) {
        if (section.getStages().isEmpty()) {
            return List.of();
        }

        List<Stage> reachableStages = new ArrayList<>(section.getStages().size());
        reachableStages.add(section.getStages().get(0));

        for (int i = 0; i < section.getStages().size() - 1; i++) {
            if (isCompleted(section.getStages().get(i), userId)) {
                // current stage is completed, so the next stage is reachable
                reachableStages.add(section.getStages().get(i + 1));
            }
        }

        return reachableStages;
    }

    /**
     * @return the user progress data for the given user and content. The result is cached.
     */
    private UserProgressData getUserProgressData(UUID userId, UUID contentId) {
        Pair<UUID, UUID> key = Pair.of(userId, contentId);
        if (userProgressDataCache.containsKey(key)) {
            return userProgressDataCache.get(key);
        }

        UserProgressData userProgressData = userProgressDataService.getUserProgressData(userId, contentId);
        userProgressDataCache.put(key, userProgressData);
        return userProgressData;
    }

    /**
     * @return whether the given stage is completed by the given user. Completion means that all required contents
     * are learned.
     */
    private boolean isCompleted(Stage stage, UUID userId) {
        return stage.getRequiredContents().stream()
                .allMatch(contentEntity -> getUserProgressData(userId, contentEntity.getId()).getIsLearned());
    }

    /**
     * @return whether the given content has one of the given skill types. If the list of skill types is empty, this
     * method always returns true. Otherwise, this method returns true if the content is an assessment and its skill
     * type is contained in the list of skill types.
     */
    private boolean hasCorrectSkillType(Content content, List<SkillType> skillTypes) {
        if (isEmpty(skillTypes)) {
            return true;
        }

        if (!(content instanceof Assessment assessment)) {
            return false;
        }

        return skillTypes.contains(assessment.getAssessmentMetadata().getSkillType());
    }

}
