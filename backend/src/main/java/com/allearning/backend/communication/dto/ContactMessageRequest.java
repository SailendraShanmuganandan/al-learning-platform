package com.allearning.backend.communication.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ContactMessageRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        String email,

        @Pattern(
                regexp = "^\\+[1-9]\\d{6,14}$|^$",
                message = "Phone must be in E.164 format (e.g. +911234567890) or left blank"
        )
        String phone,

        @NotBlank(message = "Message is required")
        @Size(min = 10, max = 2000, message = "Message must be between 10 and 2000 characters")
        String message,

        String website
) {}
