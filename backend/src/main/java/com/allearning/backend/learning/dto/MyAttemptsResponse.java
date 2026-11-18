package com.allearning.backend.learning.dto;

import org.springframework.data.domain.Page;

public record MyAttemptsResponse(
        Page<AttemptSummaryDto> attempts,
        MyAttemptsSummary summary
) {}

