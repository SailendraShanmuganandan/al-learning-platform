package com.allearning.backend.learning.dto;

import com.allearning.backend.learning.service.PerformanceLevel;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WeakAreaItemDto(
        String type, // "CHAPTER" or "TOPIC"
        Long id,
        String name,
        Long chapterId,
        String chapterName,
        int answerCount,
        Double accuracy,
        PerformanceLevel level
) {}

