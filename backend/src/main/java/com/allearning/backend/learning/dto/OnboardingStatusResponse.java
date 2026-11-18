package com.allearning.backend.learning.dto;

public record OnboardingStatusResponse(
        boolean offerInitialDiagnostic,
        String reason
) {}

