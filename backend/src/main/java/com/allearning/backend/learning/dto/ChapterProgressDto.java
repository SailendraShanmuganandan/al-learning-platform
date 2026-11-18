package com.allearning.backend.learning.dto;

import com.allearning.backend.learning.service.PerformanceLevel;
import com.allearning.backend.learning.service.PerformanceTrend;

public record ChapterProgressDto(
        Long chapterId,
        String chapterName,
        Integer chapterOrder,
        String state,
        int answerCount,
        Double accuracy,
        Integer percent,
        PerformanceLevel level,
        PerformanceTrend trend
) {}

