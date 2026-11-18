package com.allearning.backend.learning.dto;

import com.allearning.backend.learning.entity.TopicStatus;
import jakarta.validation.constraints.Size;

public record TopicUpdateRequest(
        @Size(max = 255, message = "Topic name must not exceed 255 characters")
        String name,
        TopicStatus status
) {}

