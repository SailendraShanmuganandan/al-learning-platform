package com.allearning.backend.learning.dto;

public record MistakePracticeRequest(
        Long chapterId,
        Long subjectId,
        Integer count,
        String language
) {}

