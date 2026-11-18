package com.allearning.backend.learning.dto;

import java.util.List;

public record BulkStatusResponse(
        List<Long> updatedIds,
        List<SkippedQuestion> skipped
) {
    public record SkippedQuestion(
            Long id,
            String reason
    ) {}
}

