package com.allearning.backend.learning.dto;

public record LearningResourceInfo(
        Long productId,
        String name,
        String slug,
        String productType,
        Long chapterId,
        boolean owned
) {}

