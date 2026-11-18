package com.allearning.backend.learning.dto;

import com.allearning.backend.learning.entity.QuestionReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QuestionReportCreateRequest(
        @NotNull(message = "Reason is required")
        QuestionReportReason reason,

        @Size(max = 500, message = "Comment must not exceed 500 characters")
        String comment
) {}

