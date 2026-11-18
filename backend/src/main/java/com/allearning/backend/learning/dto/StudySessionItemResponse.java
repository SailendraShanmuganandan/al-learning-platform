package com.allearning.backend.learning.dto;

import com.allearning.backend.learning.entity.StudySessionItemType;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record StudySessionItemResponse(
        Long itemId,
        int order,
        StudySessionItemType itemType,
        boolean completed,
        OffsetDateTime completedAt,
        String chapterName,
        String topicName,
        ProductResourceDto product
) {
    public record ProductResourceDto(
            Long productId,
            String name,
            String slug,
            String productType
    ) {}
}

