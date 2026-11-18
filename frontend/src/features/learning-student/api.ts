import { apiFetch } from '../../api/authApi';
import type {
  QuizAvailabilityResponse,
  QuizAttemptResponse,
  QuizAttemptSubmitRequest,
  MyAttemptsSummaryResponse,
  MistakeSummaryResponse,
  MistakeItemResponse,
  MistakePracticeRequest,
  SyllabusProgressResponse,
  RecommendationResponse,
  StudentLearningOverviewResponse,
  StudySessionResponse,
  CreateStudySessionRequest,
  DiagnosticAssessmentResponse,
  AssessmentOnboardingStatusResponse,
  AssessmentAvailableChaptersResponse,
  CreateAssessmentRequest,
  QuestionReportRequest,
  QuestionReportResponse,
  PagedResponse,
} from './types';

function toQueryString(params: object): string {
  const q = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      q.set(key, String(value));
    }
  }
  const str = q.toString();
  return str ? `?${str}` : '';
}

// ---------------------------------------------------------------------------
// Quiz Availability & Attempts
// ---------------------------------------------------------------------------

export async function getQuizAvailability(quizId: number): Promise<QuizAvailabilityResponse> {
  return apiFetch<QuizAvailabilityResponse>(`/api/quizzes/${quizId}`);
}

export async function getChapterQuizAvailability(chapterId: number): Promise<QuizAvailabilityResponse> {
  return apiFetch<QuizAvailabilityResponse>(`/api/chapters/${chapterId}/quiz`);
}

export async function startQuizAttempt(
  quizId: number,
  language?: string
): Promise<QuizAttemptResponse> {
  return apiFetch<QuizAttemptResponse>(`/api/quizzes/${quizId}/attempts`, {
    method: 'POST',
    body: JSON.stringify({ language: language || 'EN' }),
  });
}

export async function getQuizAttempt(attemptId: number): Promise<QuizAttemptResponse> {
  return apiFetch<QuizAttemptResponse>(`/api/quiz-attempts/${attemptId}`);
}

export async function submitQuizAttempt(
  attemptId: number,
  payload: QuizAttemptSubmitRequest
): Promise<QuizAttemptResponse> {
  return apiFetch<QuizAttemptResponse>(`/api/quiz-attempts/${attemptId}/submit`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function getMyQuizAttempts(
  quizId: number,
  page = 0,
  size = 10
): Promise<MyAttemptsSummaryResponse> {
  const qs = toQueryString({ page, size });
  return apiFetch<MyAttemptsSummaryResponse>(`/api/quizzes/${quizId}/my-attempts${qs}`);
}

// ---------------------------------------------------------------------------
// Mistakes
// ---------------------------------------------------------------------------

export async function getMistakesSummary(): Promise<MistakeSummaryResponse> {
  return apiFetch<MistakeSummaryResponse>('/api/me/mistakes/summary');
}

export async function getMistakes(params: {
  status?: string;
  subjectId?: number;
  chapterId?: number;
  page?: number;
  size?: number;
}): Promise<PagedResponse<MistakeItemResponse>> {
  const qs = toQueryString(params);
  return apiFetch<PagedResponse<MistakeItemResponse>>(`/api/me/mistakes${qs}`);
}

export async function startMistakePractice(
  payload: MistakePracticeRequest
): Promise<QuizAttemptResponse> {
  return apiFetch<QuizAttemptResponse>('/api/me/mistakes/practice', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

// ---------------------------------------------------------------------------
// Progress & Recommendations & Overview
// ---------------------------------------------------------------------------

export async function getSyllabusProgress(subjectId?: number | null): Promise<SyllabusProgressResponse> {
  const qs = toQueryString({ subjectId });
  return apiFetch<SyllabusProgressResponse>(`/api/me/progress${qs}`);
}

export async function getNextRecommendation(): Promise<RecommendationResponse> {
  return apiFetch<RecommendationResponse>('/api/me/recommendations/next');
}

export async function getStudentLearningOverview(): Promise<StudentLearningOverviewResponse> {
  return apiFetch<StudentLearningOverviewResponse>('/api/me/learning/overview');
}

// ---------------------------------------------------------------------------
// Study Sessions
// ---------------------------------------------------------------------------

export async function createStudySession(
  payload: CreateStudySessionRequest
): Promise<StudySessionResponse> {
  return apiFetch<StudySessionResponse>('/api/me/study-sessions', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function getStudySession(id: number): Promise<StudySessionResponse> {
  return apiFetch<StudySessionResponse>(`/api/me/study-sessions/${id}`);
}

export async function getStudySessionsList(params: {
  status?: string;
  page?: number;
  size?: number;
}): Promise<PagedResponse<StudySessionResponse>> {
  const qs = toQueryString(params);
  return apiFetch<PagedResponse<StudySessionResponse>>(`/api/me/study-sessions${qs}`);
}

export async function completeStudySessionItem(
  sessionId: number,
  itemId: number
): Promise<void> {
  return apiFetch<void>(`/api/me/study-sessions/${sessionId}/items/${itemId}/complete`, {
    method: 'PATCH',
  });
}

export async function abandonStudySession(sessionId: number): Promise<void> {
  return apiFetch<void>(`/api/me/study-sessions/${sessionId}/abandon`, {
    method: 'POST',
  });
}

// ---------------------------------------------------------------------------
// Assessments (Diagnostic & Overall Quiz)
// ---------------------------------------------------------------------------

export async function getAssessmentOnboardingStatus(): Promise<AssessmentOnboardingStatusResponse> {
  return apiFetch<AssessmentOnboardingStatusResponse>('/api/me/diagnostic-assessments/onboarding-status');
}

export async function getAvailableDiagnosticChapters(): Promise<AssessmentAvailableChaptersResponse> {
  return apiFetch<AssessmentAvailableChaptersResponse>('/api/assessments/available-chapters');
}

export async function createDiagnosticAssessment(
  payload: CreateAssessmentRequest
): Promise<DiagnosticAssessmentResponse> {
  return apiFetch<DiagnosticAssessmentResponse>('/api/diagnostic-assessments', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function getDiagnosticAssessment(id: number): Promise<DiagnosticAssessmentResponse> {
  return apiFetch<DiagnosticAssessmentResponse>(`/api/diagnostic-assessments/${id}`);
}

export async function submitDiagnosticAssessment(
  id: number,
  payload: QuizAttemptSubmitRequest
): Promise<DiagnosticAssessmentResponse> {
  return apiFetch<DiagnosticAssessmentResponse>(`/api/diagnostic-assessments/${id}/submit`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function getMyAssessments(params: {
  type?: string;
  status?: string;
  page?: number;
  size?: number;
}): Promise<PagedResponse<DiagnosticAssessmentResponse>> {
  const qs = toQueryString(params);
  return apiFetch<PagedResponse<DiagnosticAssessmentResponse>>(`/api/me/diagnostic-assessments${qs}`);
}

// ---------------------------------------------------------------------------
// Question Reports
// ---------------------------------------------------------------------------

export async function reportQuestion(
  questionId: number,
  payload: QuestionReportRequest
): Promise<QuestionReportResponse> {
  return apiFetch<QuestionReportResponse>(`/api/questions/${questionId}/reports`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

