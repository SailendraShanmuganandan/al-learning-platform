package com.allearning.backend.communication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnnouncementCreateRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 150, message = "Title must not exceed 150 characters")
        String title,

        @NotBlank(message = "Content is required")
        @Size(max = 5000, message = "Content must not exceed 5000 characters")
        String content
) {}
