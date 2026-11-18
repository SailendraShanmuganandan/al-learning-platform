package com.allearning.backend.learning.dto;

import java.util.List;

public record AdminStudentLearningDetailResponse(
        Long userId,
        String fullName,
        String email,
        List<ChapterPerformanceDto> chapterPerformances,
        WeakAreasResponse weakAreas,
        MistakeSummaryResponse mistakeSummary,
        List<RecentActivityDto> recentActivities
) {}

