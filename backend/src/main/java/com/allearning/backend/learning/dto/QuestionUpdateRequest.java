package com.allearning.backend.learning.dto;

import com.allearning.backend.learning.entity.QuestionDifficulty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record QuestionUpdateRequest(
        Long topicId,
        @NotBlank(message = "Question text cannot be empty")
        String questionText,
        String explanation,
        @NotNull(message = "Difficulty is required")
        QuestionDifficulty difficulty,
        String language,
        @NotEmpty(message = "At least two options are required")
        List<@Valid QuestionOptionRequest> options
) {}

