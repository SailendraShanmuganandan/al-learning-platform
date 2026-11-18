package com.allearning.backend.learning.service;

import java.time.OffsetDateTime;

/**
 * Service interface exposed by learning module to admin dashboard (modular monolith rule).
 */
public interface LearningAdminStatsService {

    long countPendingQuestions();

    long countOpenQuestionReports();

    long countFailedGenerationJobsSince(OffsetDateTime since);
}

