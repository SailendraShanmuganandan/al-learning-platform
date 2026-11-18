# Student Frontend Audit Notes (Phase 6)

## 1. Existing Student Frontend Architecture
- **Routing & Guards**:
  - `ProtectedRoute` (`src/components/ProtectedRoute.tsx`) wraps all authenticated student routes. Unauthenticated users are redirected to `/login` preserving `location.state.from` (or `?returnUrl=`).
  - Session is restored from `sessionStorage` via `al_session_token`.
- **Navigation & Layout**:
  - Existing student pages (`DashboardPage`, `MyVideosPage`, `ChapterDetailPage`, etc.) render a clean top-bar with AL logo, public catalog links (`Subjects`, `Notes & Videos`, `Classes`, `News`, `Contact`), student links (`My Classes`, `My Videos`, `Cart`, `Profile / Dashboard`).
  - A shared `StudentLayout` will wrap the student learning feature pages with consistent top-bar navigation, a dedicated "Learning" dropdown group, unverified-email banner, and global `al:email-not-verified` error alert.
- **Unverified-Email Handling**:
  - `apiFetch` in `src/api/authApi.ts` intercepts backend `EMAIL_NOT_VERIFIED` 403 responses and dispatches a global window event `al:email-not-verified`.
  - Pages show an inline amber banner with a "Resend verification email" action button calling `resendVerification()`.
- **Chapter Page Integration**:
  - `ChapterDetailPage` (`src/features/academic/ChapterDetailPage.tsx`) already has a designated placeholder `<section aria-labelledby="quizzes-heading">` for quizzes and practice.
  - An additive `ChapterQuizCard` component will be embedded here, calling public `GET /api/chapters/{chapterId}/quiz`.
- **Product & Resource Links**:
  - Notes: `/products/:slug`
  - Videos: `/products/:slug` (public preview/purchase) or `/my-videos/:productId` (owned playback).
- **Post-Login & Onboarding Offer**:
  - Default post-login destination for students is `/dashboard`.
  - On `/dashboard` and `/learning`, if `offerInitialDiagnostic` is true from `GET /api/me/diagnostic-assessments/onboarding-status` and not skipped in `localStorage` (`al_onboarding_skipped_${userId}`), an onboarding modal/banner will prompt the student to take the diagnostic assessment.

## 2. Directory Structure for Student Learning
- `frontend/src/features/learning-student/`
  - `api.ts`: All HTTP calls strictly mirroring `docs/api-learning.md`
  - `types.ts`: Comprehensive TypeScript interfaces mirroring backend DTOs and API specs
  - `routes.ts`: Student learning route path constants
  - `errorMapper.ts`: Student-facing error translation with contextual recovery actions
  - `recommendationHelper.ts`: `recommendationToRoute` pure mapping
  - `components/`:
    - `StudentLayout.tsx`: Shared shell with top navigation and email banners
    - `LevelBadge.tsx`: Neutral representation for `INSUFFICIENT_DATA`, `NOT_ENOUGH_DATA`, `NO_QUESTIONS`
    - `TrendIcon.tsx`: Trend indicators (`IMPROVING`, `STEADY`, `DECLINING`)
    - `DifficultyBadge.tsx`: Difficulty indicators (`EASY`, `MEDIUM`, `HARD`)
    - `ScoreRing.tsx` & `ProgressBar.tsx`: Visual progress and mastery indicators
    - `EmptyState.tsx`: Reusable friendly empty states
    - `UnverifiedEmailPrompt.tsx`: Inline verification prompt
    - `QuestionRunner.tsx`: Mobile-first single-question runner with draft persistence & navigation
    - `ResultView.tsx`: Comprehensive quiz/assessment result breakdown with question report dialog
    - `RecommendationCard.tsx`: Advisory recommendation widget
    - `QuestionReportDialog.tsx`: Modal for reporting question defects
    - `OnboardingOfferModal.tsx`: Skippable diagnostic onboarding prompt
    - `ChapterQuizCard.tsx`: Chapter page embed for quiz metadata & start
  - `pages/`:
    - `StudentLearningOverviewPage.tsx`: `/learning`
    - `StudentChapterQuizStartPage.tsx`: `/quizzes/:quizId`
    - `StudentQuizAttemptPage.tsx`: `/attempts/:attemptId`
    - `StudentMyAttemptsPage.tsx`: `/quizzes/:quizId/my-attempts`
    - `StudentMistakesPage.tsx`: `/learning/mistakes`
    - `StudentProgressPage.tsx`: `/learning/progress`
    - `StudentOverallQuizPage.tsx`: `/learning/assessments/overall`
    - `StudentDiagnosticPage.tsx`: `/learning/assessments/diagnostic`
    - `StudentAssessmentPage.tsx`: `/assessments/:assessmentId`
    - `StudentAssessmentsHistoryPage.tsx`: `/learning/assessments`
    - `StudentStudySessionStartPage.tsx`: `/learning/study`
    - `StudentStudySessionDetailPage.tsx`: `/learning/study/:sessionId`

