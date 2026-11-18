# Learning API

All learning endpoints use the standard error body:

```json
{"timestamp":"2026-11-04T10:00:00+05:30","status":409,"error":"Conflict","message":"...","code":"NOT_ENOUGH_QUESTIONS"}
```

## Student and public endpoints

### Quiz availability and attempts

#### `GET /api/quizzes/{quizId}` — public

```json
{
  "quizId": 12,
  "chapterId": 3,
  "chapterName": "Data Communication",
  "title": "Data Communication Quiz",
  "description": "Chapter assessment",
  "status": "PUBLISHED",
  "available": true,
  "unavailableReason": null,
  "minEligibleQuestions": 5,
  "questionsPerAttempt": 10,
  "languages": [{"language": "EN", "eligibleCount": 20}],
  "defaultLanguage": "EN",
  "chatbotAvailable": false,
  "published": true
}
```

Errors: `NOT_FOUND` 404 for unpublished, missing, inactive-chapter, or inactive-subject quizzes.

#### `GET /api/chapters/{chapterId}/quiz` — public

Query none. Returns the same availability shape, or:
```json
{"exists":false,"available":false,"unavailableReason":"NO_QUIZ","quizId":null,"chapterId":3,"chapterName":"Data Communication","chatbotAvailable":false}
```

#### `POST /api/quizzes/{quizId}/attempts` — STUDENT

Body: `{"language":"EN"}`. Returns an in-progress attempt:

```json
{
  "attemptId": 101,
  "attemptType": "CHAPTER_QUIZ",
  "quizId": 12,
  "chapterId": 3,
  "status": "IN_PROGRESS",
  "language": "EN",
  "totalMarks": 10,
  "startedAt": "2026-11-04T10:00:00+05:30",
  "resumed": false,
  "questions": [{
    "questionId": 501,
    "order": 1,
    "text": "Which protocol provides reliable ordered delivery?",
    "difficulty": "MEDIUM",
    "topicId": 8,
    "topicName": "Transport",
    "chapterId": 3,
    "chapterName": "Data Communication",
    "options": [{"optionId": 1501,"text":"TCP","order":1},{"optionId":1502,"text":"UDP","order":2}]
  }]
}
```

Codes: `NOT_ENOUGH_QUESTIONS` 409, `QUIZ_NOT_PUBLISHED` 404, `ATTEMPT_ALREADY_SUBMITTED` 409.

#### `GET /api/quiz-attempts/{attemptId}` — STUDENT

In-progress and completed responses. A completed response adds `score`, `totalMarks`, `percentage`, `completedAt`, `chapterBreakdown`, and result fields including `selectedOptionId`, `correctOptionId`, `isCorrect`, `explanation`, and `reportedByMe`.

#### `POST /api/quiz-attempts/{attemptId}/submit` — STUDENT

Body: `{"answers":[{"questionId":501,"selectedOptionId":1501}]}`.

Codes: `UNANSWERED_QUESTIONS` 400, `INVALID_ANSWER` 400, `ATTEMPT_ALREADY_SUBMITTED` 409, `STUDY_SESSION_ENDED` 409.

#### `GET /api/quizzes/{quizId}/my-attempts` — STUDENT

Pagination query `page`, `size`. Response includes paged completed attempts and:
```json
{"attemptCount":3,"bestPercentage":90.0,"latestPercentage":80.0,"averagePercentage":83.3}
```

### Mistakes

- `GET /api/me/mistakes?status=ACTIVE|MASTERED|ALL&subjectId=&chapterId=&page=&size`
- `GET /api/me/mistakes/summary`
- `POST /api/me/mistakes/practice`, body `{"chapterId":3,"subjectId":null,"count":10,"language":"EN"}`

Practice returns HTTP 201 with the standard in-progress attempt DTO.

Codes: `NO_MISTAKES_TO_PRACTICE` 409.

### Progress, recommendations, and overview

- `GET /api/me/progress?subjectId=` — STUDENT.
- `GET /api/me/recommendations/next` — STUDENT.
- `GET /api/me/learning/overview` — STUDENT.

Progress response:

```json
{
  "overall":{"totalChapters":6,"participatingChapters":5,"assessedChapters":4,"weakCount":1,"developingCount":2,"strongCount":1,"notEnoughDataCount":1,"coveragePercent":80.0,"averagePercent":67.5},
  "subjects":[{
    "subjectId":1,"subjectName":"ICT","summary":{"totalChapters":6,"participatingChapters":5,"assessedChapters":4,"weakCount":1,"developingCount":2,"strongCount":1,"notEnoughDataCount":1,"coveragePercent":80.0,"averagePercent":67.5},
    "chapters":[{"chapterId":3,"chapterName":"Data Communication","chapterOrder":2,"state":"DEVELOPING","answerCount":20,"accuracy":0.65,"percent":65,"level":"DEVELOPING","trend":"STEADY"}]
  }]
}
```

Recommendation response:

```json
{
  "type":"PRACTICE_MISTAKES",
  "title":"Practise your mistakes",
  "reason":"You have mistakes to practise in Data Communication.",
  "reasonCode":"PRACTICE_MISTAKES_WEAK_CHAPTER",
  "target":{"kind":"CHAPTER","chapterId":3,"chapterName":"Data Communication","topicNames":["Transport"]},
  "alternatives":[]
}
```

Overview includes onboarding, overall progress, weak areas, mistake counts, recommendation, recent activity, active study session, and `advice:null`.

### Study sessions

- `POST /api/me/study-sessions` — STUDENT, body `{"durationMinutes":20,"subjectId":1,"chapterId":null,"language":"EN"}`.
- `GET /api/me/study-sessions/{id}` — STUDENT and owner.
- `GET /api/me/study-sessions?status=&page=&size` — STUDENT.
- `PATCH /api/me/study-sessions/{id}/items/{itemId}/complete`.
- `POST /api/me/study-sessions/{id}/abandon`.

Create response:

```json
{
  "sessionId": 44,
  "durationMinutes":20,
  "status":"ACTIVE",
  "startedAt":"2026-11-04T10:00:00+05:30",
  "endsAt":"2026-11-04T10:20:00+05:30",
  "remainingSeconds":1200,
  "attemptId": 102,
  "questionCount":10,
  "items":[{"itemId":440,"order":1,"itemType":"PRACTICE","completed":false,"chapterName":"Data Communication","topicName":"Transport","product":null}],
  "result":null
}
```

Codes: `NOT_ENOUGH_QUESTIONS` 409, `STUDY_SESSION_ENDED` 409.

### Analytics and assessments

- `GET /api/me/analytics/chapters?subjectId=`
- `GET /api/me/analytics/chapters/{chapterId}`
- `GET /api/me/analytics/weak-areas`
- `POST /api/diagnostic-assessments`
- `GET /api/diagnostic-assessments/{id}`
- `POST /api/diagnostic-assessments/{id}/submit`
- `GET /api/me/diagnostic-assessments?type=&status=&page=&size`
- `GET /api/me/diagnostic-assessments/onboarding-status`
- `GET /api/assessments/available-chapters`

Assessment creation requires verified email. Create body:

```json
{"assessmentType":"INITIAL_DIAGNOSTIC","scopeType":"ALL_SUBJECTS","chapterIds":[],"subjectId":null,"language":"EN"}
```

Codes: `EMAIL_NOT_VERIFIED` 403, `NOT_ENOUGH_QUESTIONS` 409, `ASSESSMENT_ALREADY_SUBMITTED` 409.

### Question reports

#### `POST /api/questions/{questionId}/reports` — STUDENT

Body: `{"reason":"WRONG_ANSWER","comment":"The keyed answer is incorrect."}`

Response: `{"reportId":901,"status":"OPEN"}`.

Codes: `NOT_ANSWERED_BY_USER` 403, `ALREADY_REPORTED` 409.

## Admin endpoints

- `GET /api/admin/chapters/{chapterId}/topics`
- `POST /api/admin/chapters/{chapterId}/topics`
- `PATCH /api/admin/topics/{topicId}`
- `GET /api/admin/quizzes` (includes `pendingCount`)
- `GET /api/admin/quizzes/{quizId}` (includes `pendingCount`)
- `PATCH /api/admin/quizzes/{quizId}`
- `PATCH /api/admin/quizzes/{quizId}/status`
- `GET /api/admin/questions` (system-wide question bank with optional `chapterId` and `subjectId` query params; items include `chapterId`, `chapterName`, `subjectName`)
- `GET /api/admin/chapters/{chapterId}/questions`
- `GET /api/admin/questions/{questionId}`
- `POST /api/admin/chapters/{chapterId}/questions`
- `PUT /api/admin/questions/{questionId}` (frontend naming quirk: this is PUT, not PATCH)
- `PATCH /api/admin/questions/{questionId}/status`
- `POST /api/admin/questions/bulk-status`
- `GET /api/admin/question-reports?status=&questionId=&chapterId=&page=&size`
- `PATCH /api/admin/question-reports/{reportId}/status`, body `{"status":"RESOLVED"}`
- `GET /api/admin/learning-analytics/overview?subjectId=`
- `GET /api/admin/learning-analytics/chapters/{chapterId}`
- `GET /api/admin/learning-analytics/students/{userId}`

## Pagination

Spring pages serialise through the existing application format. Consumers should read `content`, `totalElements`, `totalPages`, `number`, and `size`.

## Enums

- `QuizAttemptType`: `CHAPTER_QUIZ`, `MISTAKE_PRACTICE`, `STUDY_SESSION`, `INITIAL_DIAGNOSTIC`, `OVERALL_QUIZ`
- `QuizAttemptStatus`: represented as `IN_PROGRESS` and `COMPLETED`
- `StudySessionStatus`: `ACTIVE`, `COMPLETED`, `ABANDONED`
- `StudySessionItemType`: `RESOURCE_REVIEW`, `MISTAKE_REVIEW`, `PRACTICE`
- `QuestionReportReason`: `WRONG_ANSWER`, `UNCLEAR`, `OTHER`
- `QuestionReportStatus`: `OPEN`, `RESOLVED`, `DISMISSED`
- `AssessmentType`: `INITIAL_DIAGNOSTIC`, `OVERALL_QUIZ`
- `AssessmentScopeType`: `CHAPTERS`, `SUBJECT`, `ALL_SUBJECTS`
- `PerformanceLevel`: `INSUFFICIENT_DATA`, `WEAK`, `DEVELOPING`, `STRONG`
- `PerformanceTrend`: `IMPROVING`, `STEADY`, `DECLINING`

## Verified-email endpoints

| Endpoint | Email verification required |
|---|---:|
| `POST /api/diagnostic-assessments` | Yes |
| Chapter quiz and mistake-practice start/submit | No |
| Study-session create and submit | No |
| Public quiz availability | No |
# A/L ICT Learning API

This document describes the implemented learning endpoints. JSON examples below are representative real controller DTOs, not placeholder shapes.

## Common contracts

### Error response

```json
{
  "timestamp": "2026-11-05T10:30:00Z",
  "status": 409,
  "code": "NOT_ENOUGH_QUESTIONS",
  "message": "Not enough eligible questions",
  "path": "/api/quizzes/12/attempts"
}
```

Common codes include `EMAIL_NOT_VERIFIED`, `NOT_ENOUGH_QUESTIONS`, `ATTEMPT_ALREADY_SUBMITTED`, `ASSESSMENT_ALREADY_SUBMITTED`, `STUDY_SESSION_ENDED`, `UNANSWERED_QUESTIONS`, `INVALID_ANSWER`, `NO_MISTAKES_TO_PRACTICE`, `ALREADY_REPORTED`, `NOT_ANSWERED_BY_USER`, `INVALID_STATUS_TRANSITION`, `OPTIONS_IMMUTABLE`, and `DUPLICATE_TOPIC_NAME`.

### Pagination

```json
{
  "content": [],
  "totalElements": 0,
  "totalPages": 0,
  "number": 0,
  "size": 20
}
```

### Verified-email policy

| Operation | Unverified student |
|---|---|
| Chapter quiz start | Allowed |
| Mistake practice start | Allowed |
| Study session create/start/submit | Allowed |
| Initial diagnostic creation | `403 EMAIL_NOT_VERIFIED` |
| Overall quiz creation | `403 EMAIL_NOT_VERIFIED` |

## Public/admin learning endpoints

### Student progress and recommendations

- `GET /api/me/progress` - student. Response:

```json
{"overall":{"totalChapters":1,"noQuestions":1,"insufficientData":0,"weak":0,"developing":0,"strong":0,"coveragePercent":0,"averagePercent":null},"subjects":[{"subjectId":1,"name":"ICT","summary":{"totalChapters":1,"noQuestions":1,"insufficientData":0,"weak":0,"developing":0,"strong":0,"coveragePercent":0,"averagePercent":null},"chapters":[{"chapterId":1,"chapterName":"Data Representation","state":"NO_QUESTIONS","percent":null,"answerCount":0}]}]}
```

- `GET /api/me/recommendations/next` - student.
- `GET /api/me/learning/overview` - student; `advice` is always present and `null`.

```json
{"onboarding":{"diagnosticCompleted":false},"progress":{"totalChapters":1,"noQuestions":0,"insufficientData":1,"weak":0,"developing":0,"strong":0},"weakAreas":{"chapters":[],"topics":[]},"mistakes":{"totalActive":0,"totalPracticeable":0,"totalMastered":0},"recommendation":{"type":"TAKE_DIAGNOSTIC","title":"Take the initial diagnostic","reason":"A diagnostic creates your learning baseline.","reasonCode":"NEW_STUDENT_NO_DIAGNOSTIC","target":{"kind":"DIAGNOSTIC","assessmentId":null,"chapterId":null},"alternatives":[]}, "recentActivity":[],"activeStudySession":null,"advice":null}
```

### Quizzes and attempts

- `GET /api/quizzes/{quizId}` - student/admin.
- `GET /api/chapters/{chapterId}/quiz` - public.
- `POST /api/quizzes/{quizId}/attempts` - student. Body `{ "language": "EN" }`.
- `GET /api/quizzes/{quizId}/my-attempts` - student.
- `GET /api/quiz-attempts/{attemptId}` - owner/admin; answer details are omitted while `IN_PROGRESS`.
- `POST /api/quiz-attempts/{attemptId}/submit` - owner.

Attempt submission:

```json
{
  "answers": [
    {"questionId": 101, "selectedOptionId": 1001}
  ]
}
```

### Mistakes, analytics and assessments

- `GET /api/me/mistakes` - student.
- `GET /api/me/mistakes/summary` - student.
- `POST /api/me/mistakes/practice` - student; Body `{ "chapterId": 1, "language": "EN", "count": 10 }`.
- `GET /api/me/analytics/chapters` - student.
- `GET /api/me/analytics/chapters/{chapterId}` - student.
- `GET /api/me/analytics/weak-areas` - student.
- `POST /api/diagnostic-assessments` - verified student.
- `GET /api/diagnostic-assessments/{id}` - owner.
- `POST /api/diagnostic-assessments/{id}/submit` - owner.
- `GET /api/me/diagnostic-assessments` - student.
- `GET /api/me/diagnostic-assessments/onboarding-status` - student.
- `GET /api/assessments/available-chapters` - student.

Assessment create response:

```json
{"assessmentId":77,"assessmentType":"INITIAL_DIAGNOSTIC","scopeType":"ALL_SUBJECTS","scopeReference":"{\"subjectIds\":[1]}","status":"IN_PROGRESS","score":null,"totalMarks":20,"startedAt":"2026-11-05T10:30:00+05:30","completedAt":null,"onboardingOffer":true}
```

### Study sessions

- `POST /api/me/study-sessions` - student. Returns `201`.
- `GET /api/me/study-sessions/{id}` - owner.
- `GET /api/me/study-sessions` - owner, paginated newest-first.
- `PATCH /api/me/study-sessions/{id}/items/{itemId}/complete` - owner.
- `POST /api/me/study-sessions/{id}/abandon` - owner.

Create body:

```json
{"durationMinutes":20,"subjectId":1,"chapterId":1,"language":"EN"}
```

Response:

```json
{"sessionId":55,"userId":12,"durationMinutes":20,"subjectId":1,"chapterId":1,"language":"EN","status":"ACTIVE","startedAt":"2026-11-05T10:30:00+05:30","endsAt":"2026-11-05T10:50:00+05:30","completedAt":null,"questionCount":10,"resumed":false,"items":[{"itemId":501,"itemType":"QUESTION","itemOrder":1,"questionId":101,"resourceId":null,"completed":false}]}
```

### Question reports

- `POST /api/questions/{questionId}/reports` - student with an answer for the question. `{ "reason":"WRONG_ANSWER", "comment":"Correct answer differs in the syllabus" }`.
- `GET /api/admin/question-reports` - admin; filters `status`, `questionId`, `chapterId`.
- `PATCH /api/admin/question-reports/{id}/status` - admin; `{ "status":"RESOLVED" }`.

## Admin topics, questions and quizzes

### Topics
- `GET /api/admin/chapters/{chapterId}/topics`
- `POST /api/admin/chapters/{chapterId}/topics`
- `PATCH /api/admin/topics/{topicId}`

Create topic body:

```json
{"name":"Binary Arithmetic","status":"ACTIVE"}
```

### Questions
- `GET /api/admin/questions` - optional `chapterId`, `subjectId`, `reported`, `lowCorrectRate`, `sourceDocumentRemoved`, `sourceDocumentId`, `generationJobId`.
- `GET /api/admin/chapters/{chapterId}/questions`
- `GET /api/admin/questions/{id}`
- `POST /api/admin/chapters/{chapterId}/questions`
- `PUT /api/admin/questions/{id}`
- `PATCH /api/admin/questions/{id}/status`
- `POST /api/admin/questions/bulk-status`

Create request:

```json
{"topicId":10,"questionText":"What is 1 + 1 in binary?","difficulty":"EASY","language":"EN","explanation":"Binary addition carries one to the next place.","saveMode":"APPROVED","options":[{"optionText":"10","optionOrder":1,"isCorrect":true},{"optionText":"11","optionOrder":2,"isCorrect":false},{"optionText":"2","optionOrder":3,"isCorrect":false}]}
```

Edit request uses existing-option `optionId` where an existing option is retained:

```json
{"topicId":10,"questionText":"What is 1 + 1 in binary?","difficulty":"EASY","language":"EN","explanation":"Binary addition carries one to the next place.","options":[{"optionId":1001,"optionText":"10","optionOrder":1,"isCorrect":true},{"optionId":1002,"optionText":"11","optionOrder":2,"isCorrect":false},{"optionId":null,"optionText":"2","optionOrder":3,"isCorrect":false}]}
```

Bulk status:

```json
{"questionIds":[101,102,103],"targetStatus":"APPROVED"}
```

Response:

```json
{"updatedIds":[101,102],"skipped":[{"id":103,"reason":"APPROVAL_PRECONDITIONS_FAILED"}]}
```

### Quizzes
- `GET /api/admin/quizzes`
- `GET /api/admin/quizzes/{id}`
- `PATCH /api/admin/quizzes/{id}`
- `PATCH /api/admin/quizzes/{id}/status`

Quiz DTOs include `pendingCount`.

### Admin analytics and dashboard

- `GET /api/admin/learning-analytics/overview`
- `GET /api/admin/learning-analytics/chapters/{chapterId}`
- `GET /api/admin/learning-analytics/students/{userId}`
- `GET /api/admin/dashboard/summary`

Dashboard:

```json
{"pendingActions":{"ordersAwaitingFulfillment":2,"paymentsNeedingReview":1,"pendingClassRegistrations":3,"unreadContactMessages":4,"pendingQuestions":5,"openQuestionReports":2,"failedGenerationJobs":0},"generatedAt":"2026-11-05T10:30:00+05:30"}
```

## Important enums

- Question status: `PENDING`, `APPROVED`, `ARCHIVED`.
- Difficulty: `EASY`, `MEDIUM`, `HARD`.
- Topic status: `ACTIVE`, `INACTIVE`.
- Quiz status: `DRAFT`, `PUBLISHED`, `ARCHIVED`.
- Attempt source types: `CHAPTER_QUIZ`, `MISTAKE_PRACTICE`, `STUDY_SESSION`, `INITIAL_DIAGNOSTIC`, `OVERALL_QUIZ`.
- Report reason: `WRONG_ANSWER`, `UNCLEAR`, `OTHER`.
- Report status: `OPEN`, `RESOLVED`, `DISMISSED`.

## Endpoint count

This document covers 46 endpoints implemented by the learning controllers. The dashboard summary endpoint is administered by the core dashboard controller and is included above.
