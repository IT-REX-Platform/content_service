package de.unistuttgart.iste.gits.content_service.client;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class QueryDefinitions {
    public static final String CONTENTS_FRAGMENT = """
            fragment ContentFragment on Content {
                id
                metadata {
                    name
                    tagNames
                    suggestedDate
                    type
                    chapterId
                    rewardPoints
                    courseId
                }
                ...on Assessment {
                     assessmentMetadata {
                         skillTypes
                         skillPoints
                         initialLearningInterval
                     }
                 }
                progressDataForUser(userId: $userId) {
                    userId
                    contentId
                    learningInterval
                    nextLearnDate
                    lastLearnDate
                    log {
                        timestamp
                        success
                        correctness
                        hintsUsed
                        timeToComplete
                    }
                }
            }
               
            
            """;

    public static final String CONTENTS_BY_COURSE_IDS_QUERY = CONTENTS_FRAGMENT + """
            query($courseIds: [UUID!]!, $userId: UUID!) {
                _internal_noauth_contentsByCourseIds(courseIds: $courseIds) {
                    ...ContentFragment
                }
            }
            """;

    public static final String CONTENTS_BY_CHAPTER_IDS_QUERY = CONTENTS_FRAGMENT + """
            query($chapterIds: [UUID!]!, $userId: UUID!) {
                _internal_noauth_contentsByChapterIds(chapterIds: $chapterIds) {
                    ...ContentFragment
                }
            }
            """;

    public static final String CONTENTS_BY_COURSE_ID_QUERY_NAME = "_internal_noauth_contentsByCourseIds";

    public static final String CONTENTS_BY_CHAPTER_ID_QUERY_NAME = "_internal_noauth_contentsByChapterIds";
}
