package com.allearning.backend.communication.dto;

import java.time.OffsetDateTime;

public record AnnouncementDetailResponse(
        Long id,
        String title,
        String content,
        OffsetDateTime publishedAt
) {}
