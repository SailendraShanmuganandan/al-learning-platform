package com.allearning.backend.learning.dto;

import com.allearning.backend.learning.entity.QuestionDifficulty;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StudentQuestionDto(
        Long questionId,
        int order,
        String text,
        QuestionDifficulty difficulty,
        Long topicId,
        String topicName,
        Long chapterId,
        String chapterName,
        List<StudentOptionDto> options
) {}

