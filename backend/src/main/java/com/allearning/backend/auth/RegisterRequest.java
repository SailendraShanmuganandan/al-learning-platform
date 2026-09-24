package com.allearning.backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Inbound DTO for POST /api/auth/register.
 * Validation annotations mirror the constraints enforced on the backend
 * and should be kept in sync with the Zod schema on the frontend.
 */
public record RegisterRequest(

        @NotBlank(message = "Full name is required")
        @Size(max = 150, message = "Full name must be at most 150 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,

        /**
         * Password strength rule: minimum 8 characters, at least one uppercase letter,
         * one lowercase letter, one digit, and one special character.
         */
        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d]).{8,}$",
                message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
        )
        String password,

        /**
         * Optional phone. When provided it must be in E.164 format, e.g. +911234567890.
         */
        @Pattern(
                regexp = "^\\+[1-9]\\d{6,14}$|^$",
                message = "Phone must be in E.164 format (e.g. +911234567890) or left blank"
        )
        String phone
) {
}
