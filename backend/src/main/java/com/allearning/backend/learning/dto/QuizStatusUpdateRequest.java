package com.allearning.backend.learning.dto;

import com.allearning.backend.learning.entity.QuizStatus;
import jakarta.validation.constraints.NotNull;

public record QuizStatusUpdateRequest(
        @NotNull(message = "Status is required")
        QuizStatus status
) {}

