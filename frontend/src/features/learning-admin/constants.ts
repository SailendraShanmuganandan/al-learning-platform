/**
 * Constant mirroring app.learning.minEligibleQuestions in backend.
 * Quizzes need at least 5 approved questions in one language to be available to students.
 */
export const MIN_ELIGIBLE_QUESTIONS = 5;

export const QUESTION_STATUSES = ['PENDING', 'APPROVED', 'ARCHIVED'] as const;
export const QUIZ_STATUSES = ['DRAFT', 'PUBLISHED', 'ARCHIVED'] as const;
export const QUESTION_DIFFICULTIES = ['EASY', 'MEDIUM', 'HARD'] as const;
export const QUESTION_LANGUAGES = [
  { code: 'EN', label: 'English' },
  { code: 'SI', label: 'Sinhala' },
  { code: 'TA', label: 'Tamil' },
] as const;

export const REPORT_STATUSES = ['OPEN', 'RESOLVED', 'DISMISSED'] as const;

