package com.allearning.backend.learning.dto;

import com.allearning.backend.learning.entity.QuestionReportStatus;

public record QuestionReportCreateResponse(
        Long reportId,
        QuestionReportStatus status
) {}

