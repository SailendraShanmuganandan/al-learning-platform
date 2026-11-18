package com.allearning.backend.learning.dto;

public record RecommendationCandidate(
        RecommendationType type,
        String title,
        String reasonCode,
        RecommendationTarget target
) {}

