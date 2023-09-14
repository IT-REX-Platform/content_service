# Content Service API

<details>
  <summary><strong>Table of Contents</strong></summary>

* [Query](#query)
* [Mutation](#mutation)
* [Objects](#objects)
    * [AssessmentMetadata](#assessmentmetadata)
    * [ContentMetadata](#contentmetadata)
    * [ContentMutation](#contentmutation)
    * [ContentPayload](#contentpayload)
    * [FlashcardSetAssessment](#flashcardsetassessment)
    * [MediaContent](#mediacontent)
    * [PaginationInfo](#paginationinfo)
    * [ProgressLogItem](#progresslogitem)
    * [QuizAssessment](#quizassessment)
    * [Section](#section)
    * [SectionMutation](#sectionmutation)
    * [Stage](#stage)
    * [Suggestion](#suggestion)
    * [UserProgressData](#userprogressdata)
* [Inputs](#inputs)
    * [AssessmentMetadataInput](#assessmentmetadatainput)
    * [CreateAssessmentInput](#createassessmentinput)
    * [CreateContentMetadataInput](#createcontentmetadatainput)
    * [CreateMediaContentInput](#createmediacontentinput)
    * [CreateSectionInput](#createsectioninput)
    * [CreateStageInput](#createstageinput)
    * [DateTimeFilter](#datetimefilter)
    * [IntFilter](#intfilter)
    * [Pagination](#pagination)
    * [StringFilter](#stringfilter)
    * [UpdateAssessmentInput](#updateassessmentinput)
    * [UpdateContentMetadataInput](#updatecontentmetadatainput)
    * [UpdateMediaContentInput](#updatemediacontentinput)
    * [UpdateStageInput](#updatestageinput)
* [Enums](#enums)
    * [ContentType](#contenttype)
    * [SkillType](#skilltype)
    * [SortDirection](#sortdirection)
    * [SuggestionType](#suggestiontype)
* [Scalars](#scalars)
    * [Boolean](#boolean)
    * [Date](#date)
    * [DateTime](#datetime)
    * [Float](#float)
    * [Int](#int)
    * [LocalTime](#localtime)
    * [String](#string)
    * [Time](#time)
    * [UUID](#uuid)
    * [Url](#url)
* [Interfaces](#interfaces)
    * [Assessment](#assessment)
    * [Content](#content)

</details>

## Query

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>contents</strong></td>
<td valign="top"><a href="#contentpayload">ContentPayload</a>!</td>
<td>


get all contents

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>contentsByIds</strong></td>
<td valign="top">[<a href="#content">Content</a>!]!</td>
<td>


Get contents by ids. Throws an error if any of the ids are not found.

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">ids</td>
<td valign="top">[<a href="#uuid">UUID</a>!]!</td>
<td></td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>findContentsByIds</strong></td>
<td valign="top">[<a href="#content">Content</a>]!</td>
<td>


Get contents by ids. If any of the given ids are not found, the corresponding element in the result list will be null.

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">ids</td>
<td valign="top">[<a href="#uuid">UUID</a>!]!</td>
<td></td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>contentsByChapterIds</strong></td>
<td valign="top">[[<a href="#content">Content</a>!]!]!</td>
<td>


get contents by chapter ids. Returns a list containing sublists, where each sublist contains all contents
associated with that chapter

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">chapterIds</td>
<td valign="top">[<a href="#uuid">UUID</a>!]!</td>
<td></td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>sectionsByChapterIds</strong></td>
<td valign="top">[[<a href="#section">Section</a>!]!]!</td>
<td>


Retrieves all existing sections for multiple chapters.

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">chapterIds</td>
<td valign="top">[<a href="#uuid">UUID</a>!]!</td>
<td></td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>suggestionsByChapterIds</strong></td>
<td valign="top">[<a href="#suggestion">Suggestion</a>!]!</td>
<td>

    Generates user specific suggestions for multiple chapters.

    Only content that the user can access will be considered.
    The contents will be ranked by suggested date, with the most overdue or most urgent content first.

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">chapterIds</td>
<td valign="top">[<a href="#uuid">UUID</a>!]!</td>
<td>


The ids of the chapters for which suggestions should be generated.

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">amount</td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


The amount of suggestions to generate in total.

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">skillTypes</td>
<td valign="top">[<a href="#skilltype">SkillType</a>!]!</td>
<td>


Only suggestions for these skill types will be generated.
If no skill types are given, suggestions for all skill types will be generated,
also containing suggestions for media content (which do not have a skill type).

</td>
</tr>
</tbody>
</table>

## Mutation

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>createMediaContent</strong></td>
<td valign="top"><a href="#mediacontent">MediaContent</a>!</td>
<td>


Create new media content

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">input</td>
<td valign="top"><a href="#createmediacontentinput">CreateMediaContentInput</a>!</td>
<td></td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>createAssessment</strong></td>
<td valign="top"><a href="#assessment">Assessment</a>!</td>
<td>


Create a new Assessment

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">input</td>
<td valign="top"><a href="#createassessmentinput">CreateAssessmentInput</a>!</td>
<td></td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>mutateContent</strong></td>
<td valign="top"><a href="#contentmutation">ContentMutation</a>!</td>
<td>


Modify Content

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">contentId</td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td></td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>createSection</strong></td>
<td valign="top"><a href="#section">Section</a>!</td>
<td>


Create new Section

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">input</td>
<td valign="top"><a href="#createsectioninput">CreateSectionInput</a>!</td>
<td></td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>mutateSection</strong></td>
<td valign="top"><a href="#sectionmutation">SectionMutation</a>!</td>
<td>


Modify Sections

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">sectionId</td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td></td>
</tr>
</tbody>
</table>

## Objects

### AssessmentMetadata

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>skillPoints</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


Number of skill points a student receives for completing this content

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>skillTypes</strong></td>
<td valign="top">[<a href="#skilltype">SkillType</a>!]!</td>
<td>


Type of the assessment

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>initialLearningInterval</strong></td>
<td valign="top"><a href="#int">Int</a></td>
<td>


The initial learning interval for the assessment in days.
This is the interval that is applied after the assessment is completed the first time.
Following intervals are calculated based on the previous interval and the user's performance.
If this is null, the assessment will never be scheduled for review, which
is useful for assessments that are not meant to be repeated.

</td>
</tr>
</tbody>
</table>

### ContentMetadata

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>name</strong></td>
<td valign="top"><a href="#string">String</a>!</td>
<td>


Name of the content

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>type</strong></td>
<td valign="top"><a href="#contenttype">ContentType</a>!</td>
<td>


Content type

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>suggestedDate</strong></td>
<td valign="top"><a href="#datetime">DateTime</a>!</td>
<td>


Suggested date when the content should be done

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>rewardPoints</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


Number of reward points a student receives for completing this content

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>chapterId</strong></td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td>


ID of the chapter this content is associated with

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>tagNames</strong></td>
<td valign="top">[<a href="#string">String</a>!]!</td>
<td>


TagNames this content is tagged with

</td>
</tr>
</tbody>
</table>

### ContentMutation

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>contentId</strong></td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td>


Identifier of Content

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>updateMediaContent</strong></td>
<td valign="top"><a href="#mediacontent">MediaContent</a>!</td>
<td>


Update an existing Content

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">input</td>
<td valign="top"><a href="#updatemediacontentinput">UpdateMediaContentInput</a>!</td>
<td></td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>updateAssessment</strong></td>
<td valign="top"><a href="#assessment">Assessment</a>!</td>
<td>


Update an existing Assessment

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">input</td>
<td valign="top"><a href="#updateassessmentinput">UpdateAssessmentInput</a>!</td>
<td></td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>deleteContent</strong></td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td>


Delete an existing Content, throws an error if no Content with the given id exists

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>addTagToContent</strong></td>
<td valign="top"><a href="#content">Content</a>!</td>
<td>


Add a tag to an existing content

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">tagName</td>
<td valign="top"><a href="#string">String</a></td>
<td></td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>removeTagFromContent</strong></td>
<td valign="top"><a href="#content">Content</a>!</td>
<td>


Remove a tag from an existing content

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">tagName</td>
<td valign="top"><a href="#string">String</a></td>
<td></td>
</tr>
</tbody>
</table>

### ContentPayload

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>elements</strong></td>
<td valign="top">[<a href="#content">Content</a>!]!</td>
<td>


the contents

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>pageInfo</strong></td>
<td valign="top"><a href="#paginationinfo">PaginationInfo</a>!</td>
<td>


pagination info

</td>
</tr>
</tbody>
</table>

### FlashcardSetAssessment

A set of flashcards, flashcard related fields are stored in the flashcard service.

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>assessmentMetadata</strong></td>
<td valign="top"><a href="#assessmentmetadata">AssessmentMetadata</a>!</td>
<td>


Assessment metadata

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>id</strong></td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td>


ID of the content

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>metadata</strong></td>
<td valign="top"><a href="#contentmetadata">ContentMetadata</a>!</td>
<td>


Metadata of the content

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>userProgressData</strong></td>
<td valign="top"><a href="#userprogressdata">UserProgressData</a>!</td>
<td>


Progress data of the content for the current user.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>progressDataForUser</strong></td>
<td valign="top"><a href="#userprogressdata">UserProgressData</a>!</td>
<td>


Progress data of the specified user.

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">userId</td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td></td>
</tr>
</tbody>
</table>

### MediaContent

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>id</strong></td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td>


ID of the content

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>metadata</strong></td>
<td valign="top"><a href="#contentmetadata">ContentMetadata</a>!</td>
<td>


Metadata of the content

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>userProgressData</strong></td>
<td valign="top"><a href="#userprogressdata">UserProgressData</a>!</td>
<td>


Progress data of the content for the current user.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>progressDataForUser</strong></td>
<td valign="top"><a href="#userprogressdata">UserProgressData</a>!</td>
<td>


Progress data of the specified user.

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">userId</td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td></td>
</tr>
</tbody>
</table>

### PaginationInfo

Return type for information about paginated results.

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>page</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


The current page number.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>size</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


The number of elements per page.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>totalElements</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


The total number of elements across all pages.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>totalPages</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


The total number of pages.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>hasNext</strong></td>
<td valign="top"><a href="#boolean">Boolean</a>!</td>
<td>


Whether there is a next page.

</td>
</tr>
</tbody>
</table>

### ProgressLogItem

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>timestamp</strong></td>
<td valign="top"><a href="#datetime">DateTime</a>!</td>
<td>


The date the user completed the content item.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>success</strong></td>
<td valign="top"><a href="#boolean">Boolean</a>!</td>
<td>


Whether the user completed the content item successfully.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>correctness</strong></td>
<td valign="top"><a href="#float">Float</a>!</td>
<td>


Value between 0 and 1 representing the user's correctness on the content item.
Can be null as some contents cannot provide a meaningful correctness value.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>hintsUsed</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


How many hints the user used to complete the content item.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>timeToComplete</strong></td>
<td valign="top"><a href="#int">Int</a></td>
<td>


Time in milliseconds it took the user to complete the content item.
Can be null for contents that do not measure completion time.

</td>
</tr>
</tbody>
</table>

### QuizAssessment

A quiz, quiz related fields are stored in the quiz service.

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>assessmentMetadata</strong></td>
<td valign="top"><a href="#assessmentmetadata">AssessmentMetadata</a>!</td>
<td>


Assessment metadata

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>id</strong></td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td>


ID of the content

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>metadata</strong></td>
<td valign="top"><a href="#contentmetadata">ContentMetadata</a>!</td>
<td>


Metadata of the content

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>userProgressData</strong></td>
<td valign="top"><a href="#userprogressdata">UserProgressData</a>!</td>
<td>


Progress data of the content for the current user.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>progressDataForUser</strong></td>
<td valign="top"><a href="#userprogressdata">UserProgressData</a>!</td>
<td>


Progress data of the specified user.

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">userId</td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td></td>
</tr>
</tbody>
</table>

### Section

Representation of a Section

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>id</strong></td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td>


Unique identifier of the Section Object

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>name</strong></td>
<td valign="top"><a href="#string">String</a>!</td>
<td>


Name of the Section

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>chapterId</strong></td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td>


Chapter the Section is located in

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>stages</strong></td>
<td valign="top">[<a href="#stage">Stage</a>!]!</td>
<td>


List of Stages contained in a Section

</td>
</tr>
</tbody>
</table>

### SectionMutation

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>sectionId</strong></td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td>


Identifier of the section

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>updateSectionName</strong></td>
<td valign="top"><a href="#section">Section</a>!</td>
<td>


update the name of a Section

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">name</td>
<td valign="top"><a href="#string">String</a>!</td>
<td></td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>deleteSection</strong></td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td>


delete a Section by ID

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>createStage</strong></td>
<td valign="top"><a href="#stage">Stage</a>!</td>
<td>


create new Stage in Section

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">input</td>
<td valign="top"><a href="#createstageinput">CreateStageInput</a></td>
<td></td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>updateStage</strong></td>
<td valign="top"><a href="#stage">Stage</a>!</td>
<td>


Update Content of Stage

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">input</td>
<td valign="top"><a href="#updatestageinput">UpdateStageInput</a></td>
<td></td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>deleteStage</strong></td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td>


delete Stage by ID

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">id</td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td></td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>updateStageOrder</strong></td>
<td valign="top"><a href="#section">Section</a>!</td>
<td>


update Order of Stages within a Section

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">stages</td>
<td valign="top">[<a href="#uuid">UUID</a>!]!</td>
<td></td>
</tr>
</tbody>
</table>

### Stage

Representation of a Stage

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>id</strong></td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td>


Unique identifier of the Stage Object

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>position</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


Position of the Stage within the Section

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>requiredContents</strong></td>
<td valign="top">[<a href="#content">Content</a>!]!</td>
<td>


List of Content that is labeled as required content

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>requiredContentsProgress</strong></td>
<td valign="top"><a href="#float">Float</a>!</td>
<td>


Percentage of User Progress made to required Content

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>optionalContents</strong></td>
<td valign="top">[<a href="#content">Content</a>!]!</td>
<td>


List of Content that is labeled as optional content

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>optionalContentsProgress</strong></td>
<td valign="top"><a href="#float">Float</a>!</td>
<td>


Percentage of Progress made to optional Content

</td>
</tr>
</tbody>
</table>

### Suggestion

Represents a suggestion for a user to learn new content or review old content.

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>content</strong></td>
<td valign="top"><a href="#content">Content</a>!</td>
<td>


The content that is suggested to the user.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>type</strong></td>
<td valign="top"><a href="#suggestiontype">SuggestionType</a>!</td>
<td>


The type of suggestion.

</td>
</tr>
</tbody>
</table>

### UserProgressData

Represents a user's progress on a content item.
See https://gits-enpro.readthedocs.io/en/latest/dev-manuals/gamification/userProgress.html

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>userId</strong></td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td>


The user's id.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>contentId</strong></td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td>


The id of the content item.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>log</strong></td>
<td valign="top">[<a href="#progresslogitem">ProgressLogItem</a>]!</td>
<td>


A list of entries each representing the user completing the content item.
Sorted by date in descending order.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>learningInterval</strong></td>
<td valign="top"><a href="#int">Int</a></td>
<td>


The learning interval in days for the content item.
If null, the content item is not scheduled for learning.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>nextLearnDate</strong></td>
<td valign="top"><a href="#datetime">DateTime</a></td>
<td>


The next time the content should be learned.
Calculated using the date the user completed the content item and the learning interval.
This is null if the user has not completed the content item once.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>lastLearnDate</strong></td>
<td valign="top"><a href="#datetime">DateTime</a></td>
<td>


The last time the content was learned successfully.
This is null if the user has not completed the content item once.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>isLearned</strong></td>
<td valign="top"><a href="#boolean">Boolean</a>!</td>
<td>


True if the user has completed the content item at least once successfully.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>isDueForReview</strong></td>
<td valign="top"><a href="#boolean">Boolean</a>!</td>
<td>


True if the assessment is due for review.

</td>
</tr>
</tbody>
</table>

## Inputs

### AssessmentMetadataInput

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>skillPoints</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


Number of skill points a student receives for completing this content

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>skillTypes</strong></td>
<td valign="top">[<a href="#skilltype">SkillType</a>!]!</td>
<td>


Type of the assessment

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>initialLearningInterval</strong></td>
<td valign="top"><a href="#int">Int</a></td>
<td>


The initial learning interval for the assessment in days.
This is the interval that is applied after the assessment is completed the first time.
Following intervals are calculated based on the previous interval and the user's performance.
If this is null, the assessment will never be scheduled for review, which
is useful for assessments that are not meant to be repeated.

</td>
</tr>
</tbody>
</table>

### CreateAssessmentInput

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>metadata</strong></td>
<td valign="top"><a href="#createcontentmetadatainput">CreateContentMetadataInput</a>!</td>
<td>


Metadata for the new Content

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>assessmentMetadata</strong></td>
<td valign="top"><a href="#assessmentmetadatainput">AssessmentMetadataInput</a>!</td>
<td>


Assessment metadata

</td>
</tr>
</tbody>
</table>

### CreateContentMetadataInput

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>name</strong></td>
<td valign="top"><a href="#string">String</a>!</td>
<td>


Name of the content

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>type</strong></td>
<td valign="top"><a href="#contenttype">ContentType</a>!</td>
<td>


Type of the content

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>suggestedDate</strong></td>
<td valign="top"><a href="#datetime">DateTime</a>!</td>
<td>


Suggested date when the content should be done

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>rewardPoints</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


Number of reward points a student receives for completing this content

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>chapterId</strong></td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td>


ID of the chapter this content is associated with

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>tagNames</strong></td>
<td valign="top">[<a href="#string">String</a>!]!</td>
<td>


TagNames this content is tagged with

</td>
</tr>
</tbody>
</table>

### CreateMediaContentInput

Input for creating new media content. Media specific fields are stored in the Media Service.

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>metadata</strong></td>
<td valign="top"><a href="#createcontentmetadatainput">CreateContentMetadataInput</a>!</td>
<td>


Metadata for the new Content

</td>
</tr>
</tbody>
</table>

### CreateSectionInput

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>chapterId</strong></td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td>


Chapter Section will belong to

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>name</strong></td>
<td valign="top"><a href="#string">String</a>!</td>
<td>


name given to Section

</td>
</tr>
</tbody>
</table>

### CreateStageInput

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>requiredContents</strong></td>
<td valign="top">[<a href="#uuid">UUID</a>!]!</td>
<td>


updated List of UUIDs for content labeled as required in this Stage

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>optionalContents</strong></td>
<td valign="top">[<a href="#uuid">UUID</a>!]!</td>
<td>


updated List of UUIDs for content labeled as optional in this Stage

</td>
</tr>
</tbody>
</table>

### DateTimeFilter

Filter for date values.
If multiple filters are specified, they are combined with AND.

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>after</strong></td>
<td valign="top"><a href="#datetime">DateTime</a></td>
<td>


If specified, filters for dates after the specified value.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>before</strong></td>
<td valign="top"><a href="#datetime">DateTime</a></td>
<td>


If specified, filters for dates before the specified value.

</td>
</tr>
</tbody>
</table>

### IntFilter

Filter for integer values.
If multiple filters are specified, they are combined with AND.

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>equals</strong></td>
<td valign="top"><a href="#int">Int</a></td>
<td>


An integer value to match exactly.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>greaterThan</strong></td>
<td valign="top"><a href="#int">Int</a></td>
<td>


If specified, filters for values greater than to the specified value.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>lessThan</strong></td>
<td valign="top"><a href="#int">Int</a></td>
<td>


If specified, filters for values less than to the specified value.

</td>
</tr>
</tbody>
</table>

### Pagination

Specifies the page size and page number for paginated results.

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>page</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


The page number, starting at 0.
If not specified, the default value is 0.
For values greater than 0, the page size must be specified.
If this value is larger than the number of pages, an empty page is returned.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>size</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


The number of elements per page.

</td>
</tr>
</tbody>
</table>

### StringFilter

Filter for string values.
If multiple filters are specified, they are combined with AND.

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>equals</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>


A string value to match exactly.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>contains</strong></td>
<td valign="top"><a href="#string">String</a></td>
<td>


A string value that must be contained in the field that is being filtered.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>ignoreCase</strong></td>
<td valign="top"><a href="#boolean">Boolean</a>!</td>
<td>


If true, the filter is case-insensitive.

</td>
</tr>
</tbody>
</table>

### UpdateAssessmentInput

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>metadata</strong></td>
<td valign="top"><a href="#updatecontentmetadatainput">UpdateContentMetadataInput</a>!</td>
<td>


Metadata for the new Content

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>assessmentMetadata</strong></td>
<td valign="top"><a href="#assessmentmetadatainput">AssessmentMetadataInput</a>!</td>
<td>


Assessment metadata

</td>
</tr>
</tbody>
</table>

### UpdateContentMetadataInput

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>name</strong></td>
<td valign="top"><a href="#string">String</a>!</td>
<td>


Name of the content

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>suggestedDate</strong></td>
<td valign="top"><a href="#datetime">DateTime</a>!</td>
<td>


Date when the content should be done

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>rewardPoints</strong></td>
<td valign="top"><a href="#int">Int</a>!</td>
<td>


Number of reward points a student receives for completing this content

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>chapterId</strong></td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td>


ID of the chapter this content is associated with

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>tagNames</strong></td>
<td valign="top">[<a href="#string">String</a>!]!</td>
<td>


TagNames this content is tagged with

</td>
</tr>
</tbody>
</table>

### UpdateMediaContentInput

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>metadata</strong></td>
<td valign="top"><a href="#updatecontentmetadatainput">UpdateContentMetadataInput</a>!</td>
<td>


Metadata for the new Content

</td>
</tr>
</tbody>
</table>

### UpdateStageInput

<table>
<thead>
<tr>
<th colspan="2" align="left">Field</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>id</strong></td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td>


Identifier of the Stage

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>requiredContents</strong></td>
<td valign="top">[<a href="#uuid">UUID</a>!]!</td>
<td>


updated List of UUIDs for content labeled as required in this Stage

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>optionalContents</strong></td>
<td valign="top">[<a href="#uuid">UUID</a>!]!</td>
<td>


updated List of UUIDs for content labeled as optional in this Stage

</td>
</tr>
</tbody>
</table>

## Enums

### ContentType

Type of the content

<table>
<thead>
<th align="left">Value</th>
<th align="left">Description</th>
</thead>
<tbody>
<tr>
<td valign="top"><strong>MEDIA</strong></td>
<td></td>
</tr>
<tr>
<td valign="top"><strong>FLASHCARDS</strong></td>
<td></td>
</tr>
<tr>
<td valign="top"><strong>QUIZ</strong></td>
<td></td>
</tr>
</tbody>
</table>

### SkillType

Type of the assessment

<table>
<thead>
<th align="left">Value</th>
<th align="left">Description</th>
</thead>
<tbody>
<tr>
<td valign="top"><strong>REMEMBER</strong></td>
<td></td>
</tr>
<tr>
<td valign="top"><strong>UNDERSTAND</strong></td>
<td></td>
</tr>
<tr>
<td valign="top"><strong>APPLY</strong></td>
<td></td>
</tr>
<tr>
<td valign="top"><strong>ANALYSE</strong></td>
<td></td>
</tr>
</tbody>
</table>

### SortDirection

Specifies the sort direction, either ascending or descending.

<table>
<thead>
<th align="left">Value</th>
<th align="left">Description</th>
</thead>
<tbody>
<tr>
<td valign="top"><strong>ASC</strong></td>
<td></td>
</tr>
<tr>
<td valign="top"><strong>DESC</strong></td>
<td></td>
</tr>
</tbody>
</table>

### SuggestionType

<table>
<thead>
<th align="left">Value</th>
<th align="left">Description</th>
</thead>
<tbody>
<tr>
<td valign="top"><strong>NEW_CONTENT</strong></td>
<td></td>
</tr>
<tr>
<td valign="top"><strong>REPETITION</strong></td>
<td></td>
</tr>
</tbody>
</table>

## Scalars

### Boolean

Built-in Boolean

### Date

An RFC-3339 compliant Full Date Scalar

### DateTime

A slightly refined version of RFC-3339 compliant DateTime Scalar

### Float

Built-in Float

### Int

Built-in Int

### LocalTime

24-hour clock time value string in the format `hh:mm:ss` or `hh:mm:ss.sss`.

### String

Built-in String

### Time

An RFC-3339 compliant Full Time Scalar

### UUID

A universally unique identifier compliant UUID Scalar

### Url

A Url scalar

## Interfaces

### Assessment

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>assessmentMetadata</strong></td>
<td valign="top"><a href="#assessmentmetadata">AssessmentMetadata</a>!</td>
<td>


Assessment metadata

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>id</strong></td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td>


ID of the content

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>metadata</strong></td>
<td valign="top"><a href="#contentmetadata">ContentMetadata</a>!</td>
<td>


Metadata of the content

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>userProgressData</strong></td>
<td valign="top"><a href="#userprogressdata">UserProgressData</a>!</td>
<td>


Progress data of the content for the current user.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>progressDataForUser</strong></td>
<td valign="top"><a href="#userprogressdata">UserProgressData</a>!</td>
<td>


Progress data of the specified user.

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">userId</td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td></td>
</tr>
</tbody>
</table>

### Content

<table>
<thead>
<tr>
<th align="left">Field</th>
<th align="right">Argument</th>
<th align="left">Type</th>
<th align="left">Description</th>
</tr>
</thead>
<tbody>
<tr>
<td colspan="2" valign="top"><strong>id</strong></td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td>


ID of the content

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>metadata</strong></td>
<td valign="top"><a href="#contentmetadata">ContentMetadata</a>!</td>
<td>


Metadata of the content

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>userProgressData</strong></td>
<td valign="top"><a href="#userprogressdata">UserProgressData</a>!</td>
<td>


Progress data of the content for the current user.

</td>
</tr>
<tr>
<td colspan="2" valign="top"><strong>progressDataForUser</strong></td>
<td valign="top"><a href="#userprogressdata">UserProgressData</a>!</td>
<td>


Progress data of the specified user.

</td>
</tr>
<tr>
<td colspan="2" align="right" valign="top">userId</td>
<td valign="top"><a href="#uuid">UUID</a>!</td>
<td></td>
</tr>
</tbody>
</table>
