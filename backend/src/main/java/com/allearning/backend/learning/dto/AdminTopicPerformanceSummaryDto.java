package com.allearning.backend.learning.dto;

public record AdminTopicPerformanceSummaryDto(
        Long topicId,
        String topicName,
        int studentsWithData,
        int weakCount,
        Double averageAccuracy
) {}

