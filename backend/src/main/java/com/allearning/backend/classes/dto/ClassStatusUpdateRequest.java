package com.allearning.backend.classes.dto;

import com.allearning.backend.classes.entity.ClassStatus;
import jakarta.validation.constraints.NotNull;

public record ClassStatusUpdateRequest(
        @NotNull(message = "Status is required")
        ClassStatus status
) {}
