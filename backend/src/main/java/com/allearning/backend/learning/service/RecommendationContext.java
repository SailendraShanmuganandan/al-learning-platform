package com.allearning.backend.learning.service;

import com.allearning.backend.learning.dto.ChapterPerformanceDto;
import com.allearning.backend.learning.dto.LearningResourceInfo;
import com.allearning.backend.learning.dto.MistakeSummaryResponse;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record RecommendationContext(
        Long userId,
        boolean emailVerified,
        boolean offerInitialDiagnostic,
        String onboardingReason,
        List<ChapterPerformanceDto> chapterPerformances,
        Map<Long, List<String>> weakTopicsByChapter,
        MistakeSummaryResponse mistakeSummary,
        Map<Long, Integer> activePracticeableMistakesByChapter,
        Map<Long, List<LearningResourceInfo>> resourcesByChapter,
        Map<Long, Long> availableQuizIdByChapter,
        Map<Long, OffsetDateTime> mostRecentAnswerByChapter,
        List<ChapterOrderInfo> syllabusChapters,
        double veryWeakAccuracyThreshold,
        int minMistakesForPractice
) {
    public record ChapterOrderInfo(
            Long chapterId,
            String chapterName,
            Integer chapterOrder,
            Long subjectId,
            String subjectName,
            boolean participates
    ) {}
}

