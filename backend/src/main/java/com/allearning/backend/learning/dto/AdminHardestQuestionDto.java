package com.allearning.backend.learning.dto;

import com.allearning.backend.learning.entity.QuestionStatus;

public record AdminHardestQuestionDto(
        Long questionId,
        String questionText,
        QuestionStatus status,
        long answerCount,
        Double correctRate
) {}

