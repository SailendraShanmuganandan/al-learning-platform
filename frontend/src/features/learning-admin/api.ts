import { apiFetch } from '../../api/authApi';
import type {
  PageResponse,
  QuizSummaryResponse,
  QuizDetailResponse,
  QuizUpdateRequest,
  QuizStatusUpdateRequest,
  TopicResponse,
  TopicCreateRequest,
  TopicUpdateRequest,
  QuestionSummaryResponse,
  QuestionDetailResponse,
  QuestionCreateRequest,
  QuestionUpdateRequest,
  QuestionStatusUpdateRequest,
  BulkStatusRequest,
  BulkStatusResponse,
  QuestionReportAdminResponse,
  QuestionReportStatusUpdateRequest,
  AdminLearningOverviewResponse,
  AdminChapterAnalyticsDetailResponse,
  AdminStudentLearningDetailResponse,
  QuestionStatus,
  QuestionDifficulty,
  QuestionSource,
  QuizStatus,
  QuestionReportStatus,
} from './types';

// Helper to serialize query params, skipping undefined, null, or empty string values
function toQueryString(params: object): string {
  const searchParams = new URLSearchParams();
  for (const [key, val] of Object.entries(params)) {
    if (val !== undefined && val !== null && val !== '') {
      searchParams.set(key, String(val));
    }
  }
  const str = searchParams.toString();
  return str ? `?${str}` : '';
}

// ---------------------------------------------------------------------------
// Quizzes
// ---------------------------------------------------------------------------

export interface QuizFilterParams {
  subjectId?: number | null;
  chapterId?: number | null;
  status?: QuizStatus | null;
  page?: number;
  size?: number;
}

export async function getAdminQuizzes(params: QuizFilterParams = {}): Promise<PageResponse<QuizSummaryResponse>> {
  return apiFetch<PageResponse<QuizSummaryResponse>>(`/api/admin/quizzes${toQueryString(params)}`);
}

export async function getAdminQuizDetail(id: number): Promise<QuizDetailResponse> {
  return apiFetch<QuizDetailResponse>(`/api/admin/quizzes/${id}`);
}

export async function updateAdminQuiz(id: number, payload: QuizUpdateRequest): Promise<QuizDetailResponse> {
  return apiFetch<QuizDetailResponse>(`/api/admin/quizzes/${id}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

export async function updateAdminQuizStatus(id: number, payload: QuizStatusUpdateRequest): Promise<QuizDetailResponse> {
  return apiFetch<QuizDetailResponse>(`/api/admin/quizzes/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

// ---------------------------------------------------------------------------
// Topics
// ---------------------------------------------------------------------------

export async function getAdminTopics(chapterId: number): Promise<TopicResponse[]> {
  return apiFetch<TopicResponse[]>(`/api/admin/chapters/${chapterId}/topics`);
}

export async function createAdminTopic(chapterId: number, payload: TopicCreateRequest): Promise<TopicResponse> {
  return apiFetch<TopicResponse>(`/api/admin/chapters/${chapterId}/topics`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateAdminTopic(topicId: number, payload: TopicUpdateRequest): Promise<TopicResponse> {
  return apiFetch<TopicResponse>(`/api/admin/topics/${topicId}`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

// ---------------------------------------------------------------------------
// Questions
// ---------------------------------------------------------------------------

export interface QuestionFilterParams {
  chapterId?: number | null;
  subjectId?: number | null;
  quizId?: number | null;
  topicId?: number | null;
  status?: QuestionStatus | null;
  difficulty?: QuestionDifficulty | null;
  language?: string | null;
  source?: QuestionSource | null;
  search?: string | null;
  reported?: boolean | null;
  lowCorrectRate?: boolean | null;
  sourceDocumentRemoved?: boolean | null;
  sourceDocumentId?: number | null;
  generationJobId?: number | null;
  page?: number;
  size?: number;
}

export async function getAllAdminQuestions(params: QuestionFilterParams = {}): Promise<PageResponse<QuestionSummaryResponse>> {
  return apiFetch<PageResponse<QuestionSummaryResponse>>(`/api/admin/questions${toQueryString(params)}`);
}

export async function getChapterAdminQuestions(
  chapterId: number,
  params: Omit<QuestionFilterParams, 'chapterId'> = {}
): Promise<PageResponse<QuestionSummaryResponse>> {
  return apiFetch<PageResponse<QuestionSummaryResponse>>(`/api/admin/chapters/${chapterId}/questions${toQueryString(params)}`);
}

export async function getAdminQuestionDetail(id: number): Promise<QuestionDetailResponse> {
  return apiFetch<QuestionDetailResponse>(`/api/admin/questions/${id}`);
}

export async function createAdminQuestion(chapterId: number, payload: QuestionCreateRequest): Promise<QuestionDetailResponse> {
  return apiFetch<QuestionDetailResponse>(`/api/admin/chapters/${chapterId}/questions`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function updateAdminQuestion(id: number, payload: QuestionUpdateRequest): Promise<QuestionDetailResponse> {
  return apiFetch<QuestionDetailResponse>(`/api/admin/questions/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  });
}

export async function updateAdminQuestionStatus(id: number, payload: QuestionStatusUpdateRequest): Promise<QuestionDetailResponse> {
  return apiFetch<QuestionDetailResponse>(`/api/admin/questions/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

export async function bulkUpdateQuestionStatus(payload: BulkStatusRequest): Promise<BulkStatusResponse> {
  return apiFetch<BulkStatusResponse>('/api/admin/questions/bulk-status', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

// ---------------------------------------------------------------------------
// Question Reports
// ---------------------------------------------------------------------------

export interface QuestionReportFilterParams {
  status?: QuestionReportStatus | null;
  questionId?: number | null;
  chapterId?: number | null;
  page?: number;
  size?: number;
}

export async function getAdminQuestionReports(
  params: QuestionReportFilterParams = {}
): Promise<PageResponse<QuestionReportAdminResponse>> {
  return apiFetch<PageResponse<QuestionReportAdminResponse>>(`/api/admin/question-reports${toQueryString(params)}`);
}

export async function updateAdminQuestionReportStatus(
  reportId: number,
  payload: QuestionReportStatusUpdateRequest
): Promise<QuestionReportAdminResponse> {
  return apiFetch<QuestionReportAdminResponse>(`/api/admin/question-reports/${reportId}/status`, {
    method: 'PATCH',
    body: JSON.stringify(payload),
  });
}

// ---------------------------------------------------------------------------
// Learning Analytics
// ---------------------------------------------------------------------------

export async function getAdminLearningOverview(subjectId?: number | null): Promise<AdminLearningOverviewResponse> {
  const query = subjectId ? `?subjectId=${subjectId}` : '';
  return apiFetch<AdminLearningOverviewResponse>(`/api/admin/learning-analytics/overview${query}`);
}

export async function getAdminChapterAnalytics(chapterId: number): Promise<AdminChapterAnalyticsDetailResponse> {
  return apiFetch<AdminChapterAnalyticsDetailResponse>(`/api/admin/learning-analytics/chapters/${chapterId}`);
}

export async function getAdminStudentLearning(userId: number): Promise<AdminStudentLearningDetailResponse> {
  return apiFetch<AdminStudentLearningDetailResponse>(`/api/admin/learning-analytics/students/${userId}`);
}
