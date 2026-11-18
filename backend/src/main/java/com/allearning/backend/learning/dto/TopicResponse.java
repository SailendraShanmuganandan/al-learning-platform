package com.allearning.backend.learning.dto;

import com.allearning.backend.learning.entity.TopicStatus;

import java.time.OffsetDateTime;

public record TopicResponse(
        Long topicId,
        Long chapterId,
        String name,
        TopicStatus status,
        long questionCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}

