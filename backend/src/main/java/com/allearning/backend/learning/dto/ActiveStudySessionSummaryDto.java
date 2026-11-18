package com.allearning.backend.learning.dto;

import java.time.OffsetDateTime;

public record ActiveStudySessionSummaryDto(
        Long sessionId,
        OffsetDateTime endsAt
) {}

