package com.allearning.backend.learning.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecommendationTarget(
        String kind,
        Long chapterId,
        String chapterName,
        Long subjectId,
        Long quizId,
        Long productId,
        String productSlug,
        String productType,
        Boolean owned,
        List<String> topicNames
) {
    public static RecommendationTarget ofKind(String kind) {
        return new RecommendationTarget(kind, null, null, null, null, null, null, null, null, null);
    }
}

