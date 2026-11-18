package com.allearning.backend.learning.dto;

import com.allearning.backend.learning.entity.QuestionStatus;
import jakarta.validation.constraints.NotNull;

public record QuestionStatusUpdateRequest(
        @NotNull(message = "Status is required")
        QuestionStatus status
) {}

