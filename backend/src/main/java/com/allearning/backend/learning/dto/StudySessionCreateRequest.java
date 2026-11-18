package com.allearning.backend.learning.dto;

import jakarta.validation.constraints.NotNull;

public record StudySessionCreateRequest(
        @NotNull(message = "Duration is required")
        Integer durationMinutes,
        Long subjectId,
        Long chapterId,
        String language
) {}

