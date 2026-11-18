/**
 * Route path constants for the student learning module.
 */
export const LEARNING_ROUTES = {
  OVERVIEW: '/learning',
  PROGRESS: '/learning/progress',
  MISTAKES: '/learning/mistakes',
  STUDY_SESSION_START: '/learning/study',
  STUDY_SESSION_DETAIL: (sessionId: number | string = ':sessionId') => `/learning/study/${sessionId}`,
  ASSESSMENTS_HISTORY: '/learning/assessments',
  OVERALL_QUIZ_START: '/learning/assessments/overall',
  DIAGNOSTIC_START: '/learning/assessments/diagnostic',
  ASSESSMENT_RUNNER: (assessmentId: number | string = ':assessmentId') => `/assessments/${assessmentId}`,
  QUIZ_START: (quizId: number | string = ':quizId') => `/quizzes/${quizId}`,
  QUIZ_ATTEMPT: (attemptId: number | string = ':attemptId') => `/attempts/${attemptId}`,
  MY_ATTEMPTS: (quizId: number | string = ':quizId') => `/quizzes/${quizId}/my-attempts`,
} as const;

