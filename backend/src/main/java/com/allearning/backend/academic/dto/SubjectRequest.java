package com.allearning.backend.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Inbound DTO for POST /api/admin/subjects and PATCH /api/admin/subjects/{id}.
 * Description is optional.
 */
public record SubjectRequest(

        @NotBlank(message = "Subject name is required")
        @Size(max = 200, message = "Subject name must be at most 200 characters")
        String name,

        @Size(max = 5000, message = "Description must be at most 5000 characters")
        String description
) {}
