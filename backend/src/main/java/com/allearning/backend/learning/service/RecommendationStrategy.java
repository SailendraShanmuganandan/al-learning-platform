package com.allearning.backend.learning.service;

import com.allearning.backend.learning.dto.Recommendation;

public interface RecommendationStrategy {
    Recommendation recommend(RecommendationContext ctx);
}

