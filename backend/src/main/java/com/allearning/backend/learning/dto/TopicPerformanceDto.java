package com.allearning.backend.learning.dto;

import com.allearning.backend.learning.service.PerformanceLevel;
import com.allearning.backend.learning.service.PerformanceTrend;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TopicPerformanceDto(
        Long topicId,
        String topicName,
        int answerCount,
        Double accuracy,
        PerformanceLevel level,
        PerformanceTrend trend
) {}

