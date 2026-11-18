package com.allearning.backend.learning.dto;

import com.allearning.backend.learning.entity.QuestionDifficulty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResultQuestionDto(
        Long questionId,
        int order,
        String text,
        QuestionDifficulty difficulty,
        Long topicId,
        String topicName,
        Long chapterId,
        String chapterName,
        List<StudentOptionDto> options,
        Long selectedOptionId,
        Long correctOptionId,
        Boolean isCorrect,
        String explanation,
        Boolean reportedByMe
) {
    public ResultQuestionDto(
            Long questionId,
            int order,
            String text,
            QuestionDifficulty difficulty,
            Long topicId,
            String topicName,
            Long chapterId,
            String chapterName,
            List<StudentOptionDto> options,
            Long selectedOptionId,
            Long correctOptionId,
            Boolean isCorrect,
            String explanation
    ) {
        this(questionId, order, text, difficulty, topicId, topicName, chapterId, chapterName, options, selectedOptionId, correctOptionId, isCorrect, explanation, false);
    }
}
