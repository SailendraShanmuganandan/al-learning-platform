package com.allearning.backend.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Inbound DTO for PATCH /api/admin/chapters/{id}/status.
 */
public record ChapterStatusRequest(

        @NotBlank(message = "Status is required")
        @Pattern(regexp = "ACTIVE|INACTIVE", message = "Status must be ACTIVE or INACTIVE")
        String status
) {}
