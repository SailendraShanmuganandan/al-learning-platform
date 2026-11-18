package com.allearning.backend.learning.dto;

public record QuestionOptionResponse(
        Long optionId,
        String optionText,
        int optionOrder,
        boolean isCorrect
) {}

