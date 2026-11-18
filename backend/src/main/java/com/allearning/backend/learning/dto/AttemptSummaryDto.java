package com.allearning.backend.learning.dto;

import java.time.OffsetDateTime;

public record AttemptSummaryDto(
        Long attemptId,
        int score,
        int totalMarks,
        double percentage,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt
) {}

