package com.allearning.backend.learning.dto;

import java.time.OffsetDateTime;

public record RecentActivityDto(
        String activityType,
        Long sourceId,
        String title,
        int score,
        int totalMarks,
        int percentage,
        OffsetDateTime completedAt
) {}

