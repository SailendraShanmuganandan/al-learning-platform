package com.allearning.backend.communication.dto;

import com.allearning.backend.communication.entity.ContactMessageStatus;
import jakarta.validation.constraints.NotNull;

public record ContactMessageStatusUpdateRequest(
        @NotNull(message = "Status is required")
        ContactMessageStatus status
) {}
