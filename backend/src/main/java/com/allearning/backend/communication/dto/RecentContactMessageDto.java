package com.allearning.backend.communication.dto;

import java.time.OffsetDateTime;

public record RecentContactMessageDto(
        Long messageId,
        String senderName,
        String status,
        OffsetDateTime createdAt
) {
}
