package com.allearning.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Inbound DTO for PATCH /api/users/me.
 * Only full_name and phone are accepted — email, role, and account_status are ignored.
 */
public record UpdateProfileRequest(

        @NotBlank(message = "Full name is required")
        @Size(max = 150, message = "Full name must be at most 150 characters")
        String fullName,

        /**
         * Optional. When provided must be E.164 (e.g. +911234567890) or blank to clear.
         */
        @Pattern(
                regexp = "^\\+[1-9]\\d{6,14}$|^$",
                message = "Phone must be in E.164 format (e.g. +911234567890) or left blank"
        )
        String phone
) {}
