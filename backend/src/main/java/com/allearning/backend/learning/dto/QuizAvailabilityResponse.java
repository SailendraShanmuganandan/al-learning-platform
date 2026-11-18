package com.allearning.backend.learning.dto;

import com.allearning.backend.learning.entity.QuizStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuizAvailabilityResponse(
        Long quizId,
        Long chapterId,
        String chapterName,
        String title,
        String description,
        QuizStatus status,
        boolean available,
        String unavailableReason,
        Integer minEligibleQuestions,
        Integer questionsPerAttempt,
        List<LanguageCountDto> languages,
        String defaultLanguage,
        Boolean exists,
        Boolean chatbotAvailable
) {}

