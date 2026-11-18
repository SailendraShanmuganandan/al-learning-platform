package com.allearning.backend.communication.dto;

import java.time.OffsetDateTime;

public record AnnouncementListItemResponse(
        Long id,
        String title,
        String excerpt,
        OffsetDateTime publishedAt
) {}
