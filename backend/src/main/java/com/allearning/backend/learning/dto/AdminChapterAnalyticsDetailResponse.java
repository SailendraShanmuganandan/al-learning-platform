package com.allearning.backend.learning.dto;

import java.util.List;

public record AdminChapterAnalyticsDetailResponse(
        Long chapterId,
        String chapterName,
        Long subjectId,
        String subjectName,
        List<AdminTopicPerformanceSummaryDto> topics,
        List<AdminHardestQuestionDto> hardestQuestions
) {}

