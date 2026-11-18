package com.allearning.backend.learning.dto;

import jakarta.validation.constraints.NotNull;

public record SubmitAnswerItem(
        @NotNull(message = "Question ID is required")
        Long questionId,
        @NotNull(message = "Selected option ID is required")
        Long selectedOptionId
) {}

