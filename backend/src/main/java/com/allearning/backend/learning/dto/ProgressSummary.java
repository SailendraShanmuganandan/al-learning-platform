package com.allearning.backend.learning.dto;

public record ProgressSummary(
        int totalChapters,
        int participatingChapters,
        int assessedChapters,
        int weakCount,
        int developingCount,
        int strongCount,
        int notEnoughDataCount,
        Double coveragePercent,
        Double averagePercent
) {}

