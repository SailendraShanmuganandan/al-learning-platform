export type QuestionStatus = 'PENDING' | 'APPROVED' | 'ARCHIVED';
export type QuizStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';
export type QuestionDifficulty = 'EASY' | 'MEDIUM' | 'HARD';
export type Language = 'EN' | 'SI' | 'TA';
export type QuestionSource = 'MANUAL' | 'AI';

export type QuestionReportReason = 'WRONG_ANSWER' | 'UNCLEAR' | 'OTHER';
export type QuestionReportStatus = 'OPEN' | 'RESOLVED' | 'DISMISSED';

export type PerformanceLevel = 'INSUFFICIENT_DATA' | 'WEAK' | 'DEVELOPING' | 'STRONG';
export type PerformanceTrend = 'IMPROVING' | 'STEADY' | 'DECLINING';

export type TopicStatus = 'ACTIVE' | 'INACTIVE';

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first?: boolean;
  last?: boolean;
  empty?: boolean;
}

// Quizzes
export interface QuizSummaryResponse {
  quizId: number;
  chapterId: number;
  chapterName: string;
  subjectId: number | null;
  subjectName: string | null;
  title: string;
  description: string | null;
  status: QuizStatus;
  questionCount: number;
  approvedCount: number;
  pendingCount: number;
  eligibleCount: number;
  eligibleByLanguage: Record<string, number>;
  createdAt: string;
  updatedAt: string;
}

export type QuizDetailResponse = QuizSummaryResponse;

export interface QuizUpdateRequest {
  title?: string;
  description?: string | null;
}

export interface QuizStatusUpdateRequest {
  status: QuizStatus;
}

// Topics
export interface TopicResponse {
  topicId: number;
  chapterId: number;
  name: string;
  status: TopicStatus;
  questionCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface TopicCreateRequest {
  name: string;
}

export interface TopicUpdateRequest {
  name?: string;
  status?: TopicStatus;
}

// Questions
export interface QuestionOptionResponse {
  optionId: number;
  text: string;
  order: number;
  isCorrect: boolean;
}

export interface QuestionOptionRequest {
  optionId?: number | null;
  optionText: string;
  optionOrder: number;
  isCorrect: boolean;
}

export interface QuestionCreateRequest {
  topicId?: number | null;
  questionText: string;
  difficulty: QuestionDifficulty;
  language: string;
  explanation?: string | null;
  options: Array<{
    optionText: string;
    optionOrder: number;
    isCorrect: boolean;
  }>;
  saveMode: 'PENDING' | 'APPROVED';
}

export interface QuestionUpdateRequest {
  topicId?: number | null;
  questionText: string;
  difficulty: QuestionDifficulty;
  language: string;
  explanation?: string | null;
  options: QuestionOptionRequest[];
}

export interface QuestionStatusUpdateRequest {
  status: QuestionStatus;
}

export interface BulkStatusRequest {
  questionIds: number[];
  targetStatus: QuestionStatus;
}

export interface BulkStatusSkippedItem {
  id: number;
  reason: string;
}

export interface BulkStatusResponse {
  updatedIds: number[];
  skipped: BulkStatusSkippedItem[];
}

export interface QuestionSummaryResponse {
  questionId: number;
  quizId: number;
  chapterId: number | null;
  chapterName: string | null;
  subjectName: string | null;
  topicId: number | null;
  topicName: string | null;
  questionText: string;
  difficulty: QuestionDifficulty;
  language: string;
  status: QuestionStatus;
  source: QuestionSource;
  sourceDocumentId: number | null;
  sourceDocumentRemoved: boolean;
  optionsCount: number;
  answerCount: number;
  correctRate: number | null;
  openReportCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface QuestionDetailResponse {
  questionId: number;
  quizId: number;
  topicId: number | null;
  topicName: string | null;
  questionText: string;
  explanation: string | null;
  difficulty: QuestionDifficulty;
  language: string;
  status: QuestionStatus;
  source: QuestionSource;
  sourceDocumentId: number | null;
  sourceDocumentRemoved: boolean;
  sourceExcerpt: string | null;
  createdBy: number | null;
  createdByName: string | null;
  approvedBy: number | null;
  approvedByName: string | null;
  approvedAt: string | null;
  optionsCount: number;
  answerCount: number;
  correctRate: number | null;
  openReportCount: number;
  createdAt: string;
  updatedAt: string;
  options: QuestionOptionResponse[];
}

// Question Reports
export interface QuestionReportAdminResponse {
  reportId: number;
  questionId: number;
  questionText: string;
  questionStatus: QuestionStatus;
  quizId: number;
  chapterId: number;
  chapterName: string;
  reporterUserId: number;
  reporterName: string;
  reason: QuestionReportReason;
  comment: string | null;
  status: QuestionReportStatus;
  createdAt: string;
  updatedAt: string;
}

export interface QuestionReportStatusUpdateRequest {
  status: QuestionReportStatus;
}

// Admin Learning Analytics
export interface AdminChapterPerformanceSummaryDto {
  chapterId: number;
  chapterName: string;
  chapterOrder: number;
  subjectId: number;
  subjectName: string;
  studentsWithData: number;
  weakCount: number;
  developingCount: number;
  strongCount: number;
  weakPercentage: number;
  averageAccuracy: number | null;
  totalGradedAnswers: number;
}

export interface AdminLearningOverviewResponse {
  subjectId: number | null;
  totalStudentsWithData: number;
  totalGradedAnswers: number;
  chapters: AdminChapterPerformanceSummaryDto[];
}

export interface AdminTopicPerformanceSummaryDto {
  topicId: number;
  topicName: string;
  studentsWithData: number;
  weakCount: number;
  averageAccuracy: number | null;
}

export interface AdminHardestQuestionDto {
  questionId: number;
  questionText: string;
  topicName: string | null;
  difficulty: QuestionDifficulty;
  status: QuestionStatus;
  answerCount: number;
  correctRate: number;
}

export interface AdminChapterAnalyticsDetailResponse {
  chapterId: number;
  chapterName: string;
  subjectId: number;
  subjectName: string;
  topics: AdminTopicPerformanceSummaryDto[];
  hardestQuestions: AdminHardestQuestionDto[];
}

export interface AdminStudentChapterPerformanceDto {
  chapterId: number;
  chapterName: string;
  subjectName: string;
  level: PerformanceLevel;
  trend: PerformanceTrend | null;
  answerCount: number;
  accuracy: number | null;
}

export interface WeakAreaItemDto {
  type: string;
  id: number;
  name: string;
  chapterId: number;
  chapterName: string;
  answerCount: number;
  accuracy: number;
  level: PerformanceLevel;
}

export interface RecentActivityDto {
  type: string;
  title: string;
  chapterName: string;
  score: number | null;
  totalMarks: number | null;
  accuracy: number | null;
  completedAt: string;
}

export interface AdminStudentLearningDetailResponse {
  userId: number;
  fullName: string;
  email: string;
  isEmailVerified: boolean;
  totalAnswerCount: number;
  chapterPerformances: AdminStudentChapterPerformanceDto[];
  weakAreas: WeakAreaItemDto[];
  activeMistakeCount: number;
  resolvedMistakeCount: number;
  recentActivities: RecentActivityDto[];
}
