package com.allearning.backend.learning.dto;

import java.util.List;

public record AdminLearningOverviewResponse(
        long totalStudentsWithAnyData,
        long totalGradedAnswers,
        List<AdminChapterPerformanceSummaryDto> chapters
) {}

