package com.allearning.backend.learning.dto;

public record QuestionAnswerStats(
        Long questionId,
        long totalAnswers,
        long correctAnswers
) {
    public long answerCount() { return totalAnswers; }
    public long correctCount() { return correctAnswers; }
}
