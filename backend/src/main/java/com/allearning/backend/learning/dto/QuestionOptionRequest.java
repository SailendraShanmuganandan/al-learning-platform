package com.allearning.backend.learning.dto;

import jakarta.validation.constraints.NotBlank;

public record QuestionOptionRequest(
        Long optionId,
        @NotBlank(message = "Option text cannot be empty")
        String optionText,
        int optionOrder,
        boolean isCorrect
) {}

