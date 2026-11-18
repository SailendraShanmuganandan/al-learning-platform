export type QuizAttemptType =
  | 'CHAPTER_QUIZ'
  | 'MISTAKE_PRACTICE'
  | 'STUDY_SESSION'
  | 'INITIAL_DIAGNOSTIC'
  | 'OVERALL_QUIZ';

export type QuizAttemptStatus = 'IN_PROGRESS' | 'COMPLETED';

export type StudySessionStatus = 'ACTIVE' | 'COMPLETED' | 'ABANDONED';

export type StudySessionItemType = 'RESOURCE_REVIEW' | 'MISTAKE_REVIEW' | 'PRACTICE';

export type QuestionReportReason = 'WRONG_ANSWER' | 'UNCLEAR' | 'OTHER';

export type QuestionReportStatus = 'OPEN' | 'RESOLVED' | 'DISMISSED';

export type AssessmentType = 'INITIAL_DIAGNOSTIC' | 'OVERALL_QUIZ';

export type AssessmentScopeType = 'CHAPTERS' | 'SUBJECT' | 'ALL_SUBJECTS';

export type PerformanceLevel = 'INSUFFICIENT_DATA' | 'WEAK' | 'DEVELOPING' | 'STRONG';

export type PerformanceTrend = 'IMPROVING' | 'STEADY' | 'DECLINING';

export type QuestionDifficulty = 'EASY' | 'MEDIUM' | 'HARD';

export interface QuizLanguageInfo {
  language: string;
  eligibleCount: number;
}

export interface QuizAvailabilityResponse {
  exists: boolean;
  quizId: number | null;
  chapterId: number;
  chapterName: string;
  title?: string;
  description?: string | null;
  status?: string;
  available: boolean;
  unavailableReason: string | null;
  minEligibleQuestions?: number;
  questionsPerAttempt?: number;
  languages?: QuizLanguageInfo[];
  defaultLanguage?: string;
  chatbotAvailable?: boolean;
  published?: boolean;
}

export interface QuizOptionDto {
  optionId: number;
  text: string;
  order: number;
}

export interface QuizAttemptQuestionDto {
  questionId: number;
  order: number;
  text: string;
  difficulty: QuestionDifficulty;
  topicId: number | null;
  topicName: string | null;
  chapterId: number;
  chapterName: string;
  options: QuizOptionDto[];
  // Completed result fields:
  selectedOptionId?: number | null;
  correctOptionId?: number;
  isCorrect?: boolean;
  explanation?: string | null;
  reportedByMe?: boolean;
}

export interface ChapterBreakdownDto {
  chapterId: number;
  chapterName: string;
  totalQuestions: number;
  correctCount: number;
  accuracy: number;
  percentage?: number;
}

export interface QuizAttemptResponse {
  attemptId: number;
  attemptType: QuizAttemptType;
  quizId?: number | null;
  chapterId?: number | null;
  studySessionId?: number | null;
  status: QuizAttemptStatus;
  language: string;
  totalMarks: number;
  score?: number | null;
  percentage?: number | null;
  startedAt: string;
  completedAt?: string | null;
  resumed?: boolean;
  questions: QuizAttemptQuestionDto[];
  chapterBreakdown?: ChapterBreakdownDto[];
}

export interface QuizAnswerSubmission {
  questionId: number;
  selectedOptionId: number;
}

export interface QuizAttemptSubmitRequest {
  answers: QuizAnswerSubmission[];
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface MyAttemptsSummaryResponse {
  attemptCount: number;
  bestPercentage: number | null;
  latestPercentage: number | null;
  averagePercentage: number | null;
  attempts: PagedResponse<QuizAttemptResponse>;
}

export interface MistakeSummaryResponse {
  activeCount: number;
  practiceableCount: number;
  masteredCount: number;
  totalMistakes: number;
  byChapter: {
    chapterId: number;
    chapterName: string;
    subjectId: number;
    subjectName: string;
    activeCount: number;
    practiceableCount: number;
    masteredCount: number;
  }[];
}

export interface MistakeItemResponse {
  mistakeId: number;
  questionId: number;
  questionText: string;
  topicId: number | null;
  topicName: string | null;
  chapterId: number;
  chapterName: string;
  subjectId: number;
  subjectName: string;
  consecutiveCorrect: number;
  status: 'ACTIVE' | 'MASTERED';
  lastPracticedAt: string | null;
  practiceable: boolean;
}

export interface MistakePracticeRequest {
  chapterId?: number | null;
  subjectId?: number | null;
  count?: number;
  language?: string;
}

export interface ChapterProgressDto {
  chapterId: number;
  chapterName: string;
  chapterOrder: number;
  state: PerformanceLevel;
  level: PerformanceLevel;
  answerCount: number;
  accuracy: number | null;
  percent: number | null;
  trend: PerformanceTrend | null;
  quizAvailable?: boolean;
  quizId?: number | null;
}

export interface SubjectProgressSummaryDto {
  totalChapters: number;
  participatingChapters: number;
  assessedChapters: number;
  weakCount: number;
  developingCount: number;
  strongCount: number;
  notEnoughDataCount: number;
  coveragePercent: number;
  averagePercent: number | null;
}

export interface SubjectProgressDto {
  subjectId: number;
  subjectName: string;
  summary: SubjectProgressSummaryDto;
  chapters: ChapterProgressDto[];
}

export interface SyllabusProgressResponse {
  overall: SubjectProgressSummaryDto;
  subjects: SubjectProgressDto[];
}

export interface RecommendationTarget {
  kind: 'CHAPTER' | 'SUBJECT' | 'SUBJECT_LIST' | 'PRODUCT' | 'VIDEO' | 'QUIZ';
  chapterId?: number | null;
  chapterName?: string | null;
  subjectId?: number | null;
  subjectName?: string | null;
  productId?: number | null;
  slug?: string | null;
  quizId?: number | null;
  topicNames?: string[];
}

export interface RecommendationResponse {
  type:
    | 'TAKE_DIAGNOSTIC'
    | 'EXPLORE_CHAPTER'
    | 'PRACTICE_MISTAKES'
    | 'REVIEW_RESOURCE'
    | 'TAKE_CHAPTER_QUIZ'
    | 'NEXT_CHAPTER'
    | 'TAKE_OVERALL_QUIZ';
  title: string;
  reason: string;
  reasonCode: string;
  target: RecommendationTarget;
  alternatives?: {
    type: string;
    title: string;
    reason: string;
    target: RecommendationTarget;
  }[];
}

export interface WeakAreaItem {
  chapterId: number;
  chapterName: string;
  subjectName: string;
  accuracy: number;
  weakTopics?: string[];
  quizId?: number | null;
}

export interface RecentActivityItem {
  attemptId: number;
  attemptType: QuizAttemptType;
  title: string;
  score: number;
  totalMarks: number;
  percentage: number;
  completedAt: string;
}

export interface StudentLearningOverviewResponse {
  offerInitialDiagnostic: boolean;
  recommendation: RecommendationResponse | null;
  progress: SubjectProgressSummaryDto | null;
  weakAreas: WeakAreaItem[];
  mistakes: {
    activeCount: number;
    practiceableCount: number;
  };
  recentActivity: RecentActivityItem[];
  activeStudySession: {
    sessionId: number;
    endsAt: string;
    remainingSeconds: number;
    questionCount: number;
    attemptId?: number | null;
  } | null;
  advice: string | null;
}

export interface CreateStudySessionRequest {
  durationMinutes: number;
  subjectId?: number | null;
  chapterId?: number | null;
  language?: string;
}

export interface StudySessionItemProductDto {
  productId: number;
  name: string;
  type: 'NOTE' | 'VIDEO';
  slug: string;
  owned: boolean;
}

export interface StudySessionItemResponse {
  itemId: number;
  order: number;
  itemType: StudySessionItemType;
  completed: boolean;
  chapterName?: string | null;
  topicName?: string | null;
  product?: StudySessionItemProductDto | null;
}

export interface StudySessionResponse {
  sessionId: number;
  durationMinutes: number;
  status: StudySessionStatus;
  startedAt: string;
  endsAt: string;
  remainingSeconds: number;
  attemptId: number | null;
  questionCount: number;
  items: StudySessionItemResponse[];
  result?: {
    score: number;
    totalMarks: number;
    percentage: number;
  } | null;
  resumed?: boolean;
}

export interface CreateAssessmentRequest {
  assessmentType: AssessmentType;
  scopeType: AssessmentScopeType;
  chapterIds?: number[];
  subjectId?: number | null;
  language?: string;
}

export interface DiagnosticAssessmentResponse {
  assessmentId: number;
  assessmentType: AssessmentType;
  scopeType: AssessmentScopeType;
  status: QuizAttemptStatus;
  language: string;
  totalMarks: number;
  score?: number | null;
  percentage?: number | null;
  startedAt: string;
  completedAt?: string | null;
  questions: QuizAttemptQuestionDto[];
  chapterBreakdown?: ChapterBreakdownDto[];
}

export interface AssessmentOnboardingStatusResponse {
  completed: boolean;
  offerInitialDiagnostic: boolean;
}

export interface AvailableChapterDto {
  chapterId: number;
  chapterName: string;
  subjectId: number;
  subjectName: string;
  eligibleQuestionsCount: number;
}

export interface AssessmentAvailableChaptersResponse {
  chapters: AvailableChapterDto[];
}

export interface QuestionReportRequest {
  reason: QuestionReportReason;
  comment?: string;
}

export interface QuestionReportResponse {
  reportId: number;
  status: QuestionReportStatus;
}

