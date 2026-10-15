package com.allearning.backend.academic.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

/**
 * Inbound DTO for POST /api/admin/chapters.
 * {@code chapterOrder} is optional — when absent the service appends the chapter
 * at the end of the existing list for the given subject.
 * {@code subjectId} cannot be changed after creation.
 */
public record ChapterCreateRequest(

        @NotNull(message = "Subject ID is required")
        Long subjectId,

        @NotBlank(message = "Chapter name is required")
        @Size(max = 300, message = "Chapter name must be at most 300 characters")
        String name,

        /** Positive 1-based position. Omit to auto-append. */
        @Positive(message = "Chapter order must be a positive integer")
        Integer chapterOrder,

        @Size(max = 10000, message = "Description must be at most 10 000 characters")
        String description
) {}
