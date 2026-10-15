package com.allearning.backend.academic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Inbound DTO for PATCH /api/admin/chapters/{id}.
 * Only name, chapterOrder, and description may be changed.
 * subjectId is intentionally absent — it cannot be changed after creation.
 */
public record ChapterUpdateRequest(

        @NotBlank(message = "Chapter name is required")
        @Size(max = 300, message = "Chapter name must be at most 300 characters")
        String name,

        @Positive(message = "Chapter order must be a positive integer")
        Integer chapterOrder,

        @Size(max = 10000, message = "Description must be at most 10 000 characters")
        String description
) {}
