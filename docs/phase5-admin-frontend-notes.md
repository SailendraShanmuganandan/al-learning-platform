# Admin Frontend Notes (Phase 5)

## 1. api-learning.md Verification & Spot Check
- Verified file `docs/api-learning.md` exists.
- Spot-checked endpoints against controllers and DTOs:
  1. `GET /api/admin/quizzes` -> Matched `AdminQuizController.java` (`QuizSummaryResponse` includes `pendingCount`).
  2. `GET /api/admin/questions` -> Matched `AdminQuestionController.java` (cross-chapter question bank with `subjectId`/`chapterId` filter, items enriched with `chapterId`, `chapterName`, `subjectName`).
  3. `GET /api/admin/chapters/{chapterId}/questions` -> Matched `AdminQuestionController.java`.
  4. `POST /api/admin/chapters/{chapterId}/questions` -> Matched `AdminQuestionController.java` (201 Created).
  5. `PUT /api/admin/questions/{id}` -> Matched `AdminQuestionController.java` (200 OK).
  6. `GET /api/admin/learning-analytics/overview` -> Matched `AdminLearningAnalyticsController.java`.
- Discrepancy corrections made:
  - Documented `pendingCount` on quiz responses.
  - Documented system-wide `GET /api/admin/questions`.
  - Documented `PUT /api/admin/questions/{id}` (authoritative code uses `PUT`, not `PATCH`).

## 2. Admin Routes Created
- `/admin/quizzes` — `AdminQuizzesPage`: Quiz management, eligibility metrics, publish/draft/archive status transitions.
- `/admin/question-bank` — `AdminQuestionBankPage`: System-wide and chapter-scoped question bank, filters, search, bulk actions.
- `/admin/question-bank/new` — `AdminQuestionFormPage`: Authoring new questions with auto-draft quiz creation.
- `/admin/question-bank/:id` — `AdminQuestionFormPage`: Question editing, lock checks for answered questions, "Archive and Create Replacement" flow.
- `/admin/question-reports` — `AdminQuestionReportsPage`: Student problem report triage, resolving, dismissing.
- `/admin/learning-analytics` — `AdminLearningOverviewPage`: System-wide syllabus mastery metrics, chapter difficulty rankings, student jump search.
- `/admin/learning-analytics/chapters/:chapterId` — `AdminChapterAnalyticsPage`: Chapter deep dive with topic accuracy and problematic question analysis.
- `/admin/learning-analytics/students/:userId` — `AdminStudentLearningPage`: Individual student learning records, trend indicators, weak chapters.

## 3. Backend Behaviour That Is Awkward for the UI
1. **Option Immutability on Answered Questions**: Once a student has submitted an attempt answering a question, the options and `isCorrect` flag are completely immutable (returns 409 `OPTIONS_IMMUTABLE`). The UI cannot simply edit options; it must guide the admin to archive the question and prefill a replacement clone via `/admin/question-bank/new`.
2. **Auto-Draft Quiz Creation**: Adding a question requires a quiz to exist in the database; the backend auto-creates a DRAFT quiz on first question submission. The UI had to clarify this to avoid admin confusion about "creating a quiz" first.
3. **Chapter-Scoped Topics**: Topics are strictly scoped to a chapter (`/api/admin/chapters/{chapterId}/topics`). When browsing questions across all chapters, the topic filter cannot be populated until a specific chapter is selected.
4. **Bulk Status Response**: Bulk updating questions (`POST /api/admin/questions/bulk-status`) returns partial success as `{ updatedIds, skipped: [{ id, reason }] }` rather than failing atomically. The request uses `questionIds` and `targetStatus`; the UI's `BulkResultDialog` explains which IDs were skipped and why.
5. **Dashboard Counts**: `GET /api/admin/dashboard` pending actions originally lacked `pendingQuestions` and `openQuestionReports` fields, requiring the frontend to safely optional-chain those metrics.

## 4. Manual Smoke-Test Checklist (10 Steps)
1. **Quizzes List**: Navigate to `/admin/quizzes`. Verify quizzes load with `approvedCount`, `pendingCount`, `eligibleCount`, and status badges.
2. **Quiz Status Transition**: On a quiz with < 5 eligible questions, attempt to publish. Confirm warning appears explaining minimum threshold requirement. Revert or publish appropriately.
3. **Topics Management**: Navigate to `/admin/question-bank`, choose a chapter, click "Manage Topics". Add a new topic, rename it, toggle active/inactive status.
4. **Author New Question**: Click "Add Question", select chapter, fill in question text, select topic, specify 4 options with exactly one correct option. Click "Save as Pending" and verify redirect to question bank with PENDING badge.
5. **Question Approval**: From `/admin/question-bank`, locate the pending question, click "Approve" (or use bulk checkbox and "Approve Selected"). Verify question status changes to APPROVED.
6. **Edit Question & Locking**: Open an approved question that has 0 student attempts. Edit options and verify save succeeds. If answering exists, verify options are locked and "Archive and Create Replacement" button appears.
7. **Filter & Search Questions**: Use Question Bank filter bar to filter by language (`EN`/`SI`/`TA`), difficulty (`MEDIUM`), and search text. Verify results update accurately.
8. **Question Reports Review**: Navigate to `/admin/question-reports`. Click on an open report, verify question preview and reporter comment, and resolve or dismiss the report.
9. **Learning Analytics Overview**: Navigate to `/admin/learning-analytics`. Verify syllabus mastery distribution, top weak chapters, and filter by subject.
10. **Student Analytics Drilldown**: In the student search box, type a known user ID (e.g. `2`) and press "Go". Verify `/admin/learning-analytics/students/2` displays student level, mastery trend, and chapter breakdown.
