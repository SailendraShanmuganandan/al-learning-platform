package com.allearning.backend.learning.dto;

import com.allearning.backend.learning.entity.QuestionReportStatus;
import jakarta.validation.constraints.NotNull;

public record QuestionReportStatusUpdateRequest(
        @NotNull(message = "Status is required")
        QuestionReportStatus status
) {}

