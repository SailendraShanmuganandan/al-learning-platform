package com.allearning.backend.learning.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LearningOverviewResponse(
        OnboardingStatusResponse onboarding,
        ProgressSummary progress,
        WeakAreasOverview weakAreas,
        MistakesOverview mistakes,
        Recommendation recommendation,
        List<RecentActivityDto> recentActivity,
        ActiveStudySessionSummaryDto activeStudySession,
        @JsonInclude(JsonInclude.Include.ALWAYS)
        Object advice
) {
    public record WeakAreasOverview(
            List<ChapterPerformanceDto> chapters,
            List<WeakTopicItemDto> topics
    ) {}

    public record WeakTopicItemDto(
            Long topicId,
            String topicName,
            Long chapterId,
            String chapterName,
            int answerCount,
            Double accuracy
    ) {}

    public record MistakesOverview(
            long totalActive,
            long totalPracticeable,
            long totalMastered
    ) {}
}
