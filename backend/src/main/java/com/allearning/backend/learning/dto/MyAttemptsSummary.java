package com.allearning.backend.learning.dto;

public record MyAttemptsSummary(
        long attemptCount,
        Double bestPercentage,
        Double latestPercentage,
        Double averagePercentage
) {}

