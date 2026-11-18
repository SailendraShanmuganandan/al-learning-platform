package com.allearning.backend.communication.dto;

import com.allearning.backend.communication.entity.ContactMessageStatus;
import java.time.OffsetDateTime;

public record AdminContactMessageListItem(
        Long id,
        String name,
        String email,
        String phone,
        String messageExcerpt,
        ContactMessageStatus status,
        OffsetDateTime createdAt
) {}
