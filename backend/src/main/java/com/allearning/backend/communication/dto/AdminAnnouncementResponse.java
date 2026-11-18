package com.allearning.backend.communication.dto;

import com.allearning.backend.communication.entity.AnnouncementStatus;
import java.time.OffsetDateTime;

public record AdminAnnouncementResponse(
        Long id,
        String title,
        String content,
        AnnouncementStatus status,
        Long createdBy,
        String createdByName,
        OffsetDateTime publishedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
