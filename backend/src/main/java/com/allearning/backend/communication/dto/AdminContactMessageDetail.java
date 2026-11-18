package com.allearning.backend.communication.dto;

import com.allearning.backend.communication.entity.ContactMessageStatus;
import java.time.OffsetDateTime;

public record AdminContactMessageDetail(
        Long id,
        String name,
        String email,
        String phone,
        String message,
        ContactMessageStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
