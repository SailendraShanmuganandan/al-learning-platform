package com.allearning.backend.learning.service;

/**
 * Hook interface called by QuizAttemptService before submitting a STUDY_SESSION attempt.
 * Decouples QuizAttemptService from StudySessionService to avoid circular dependencies.
 */
public interface StudySessionSubmissionGuard {
    /**
     * Asserts that the study session is ACTIVE and within its allowed time + grace.
     * Throws 409 STUDY_SESSION_ENDED if closed or expired.
     */
    void assertOpen(Long studySessionId);
}

