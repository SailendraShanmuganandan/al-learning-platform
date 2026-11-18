package com.allearning.backend.communication.dto;

import com.allearning.backend.communication.entity.AnnouncementStatus;
import jakarta.validation.constraints.NotNull;

public record AnnouncementStatusUpdateRequest(
        @NotNull(message = "Status is required")
        AnnouncementStatus status
) {}
