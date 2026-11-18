package com.allearning.backend.learning.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecommendationAlternative(
        RecommendationType type,
        String title,
        String reason,
        String reasonCode,
        RecommendationTarget target
) {}

