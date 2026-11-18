package com.allearning.backend.learning.dto;

import com.allearning.backend.learning.service.PerformanceLevel;
import com.allearning.backend.learning.service.PerformanceTrend;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChapterPerformanceDto(
        Long chapterId,
        String chapterName,
        Long subjectId,
        String subjectName,
        boolean participates,
        int answerCount,
        Double accuracy,
        PerformanceLevel level,
        PerformanceTrend trend
) {}

