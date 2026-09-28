package com.allearning.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Inbound DTO for POST /api/addresses and PATCH /api/addresses/{id}.
 * All fields except address_line_2 are required.
 */
public record AddressRequest(

        @NotBlank(message = "Recipient name is required")
        @Size(max = 150, message = "Recipient name must be at most 150 characters")
        String recipientName,

        @NotBlank(message = "Phone is required")
        @Pattern(
                regexp = "^\\+[1-9]\\d{6,14}$",
                message = "Phone must be in E.164 format (e.g. +911234567890)"
        )
        String phone,

        @NotBlank(message = "Address line 1 is required")
        @Size(max = 255, message = "Address line 1 must be at most 255 characters")
        String addressLine1,

        @Size(max = 255, message = "Address line 2 must be at most 255 characters")
        String addressLine2,

        @NotBlank(message = "City is required")
        @Size(max = 100, message = "City must be at most 100 characters")
        String city,

        @NotBlank(message = "District is required")
        @Size(max = 100, message = "District must be at most 100 characters")
        String district,

        @NotBlank(message = "Postal code is required")
        @Size(max = 20, message = "Postal code must be at most 20 characters")
        String postalCode,

        /**
         * Optional. When true the new address becomes the user's default; previous default is unset.
         */
        Boolean isDefault
) {}
