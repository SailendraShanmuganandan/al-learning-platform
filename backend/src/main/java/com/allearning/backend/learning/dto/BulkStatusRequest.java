package com.allearning.backend.learning.dto;

import com.allearning.backend.learning.entity.QuestionStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BulkStatusRequest(
        @NotEmpty(message = "Question IDs list cannot be empty")
        List<Long> questionIds,
        @NotNull(message = "Target status is required")
        QuestionStatus targetStatus
) {}

