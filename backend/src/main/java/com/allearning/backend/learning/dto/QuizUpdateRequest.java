package com.allearning.backend.learning.dto;

import jakarta.validation.constraints.Size;

public record QuizUpdateRequest(
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,
        String description
) {}

