package com.allearning.backend.learning.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Recommendation(
        RecommendationType type,
        String title,
        String reason,
        String reasonCode,
        RecommendationTarget target,
        List<RecommendationAlternative> alternatives
) {}

