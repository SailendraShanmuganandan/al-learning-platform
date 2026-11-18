package com.allearning.backend.classes.dto;

import com.allearning.backend.classes.entity.RegistrationStatus;
import jakarta.validation.constraints.NotNull;

public record RegistrationStatusUpdateRequest(
        @NotNull(message = "Status is required")
        RegistrationStatus status
) {}
