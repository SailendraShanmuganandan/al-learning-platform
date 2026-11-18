package com.allearning.backend.learning.dto;

import com.allearning.backend.learning.entity.QuestionReportReason;
import com.allearning.backend.learning.entity.QuestionReportStatus;
import com.allearning.backend.learning.entity.QuestionStatus;

import java.time.OffsetDateTime;

public record QuestionReportAdminResponse(
        Long reportId,
        Long questionId,
        String questionText,
        QuestionStatus questionStatus,
        Long chapterId,
        String chapterName,
        QuestionReportReason reason,
        String comment,
        QuestionReportStatus status,
        ReporterDto reporter,
        OffsetDateTime createdAt,
        Long resolvedBy,
        OffsetDateTime resolvedAt
) {
    public record ReporterDto(
            Long userId,
            String fullName
    ) {}
}

