package com.allearning.backend.learning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TopicCreateRequest(
        @NotBlank(message = "Topic name is required")
        @Size(max = 255, message = "Topic name must not exceed 255 characters")
        String name
) {}

