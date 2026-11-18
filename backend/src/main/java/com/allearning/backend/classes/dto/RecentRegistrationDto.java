package com.allearning.backend.classes.dto;

import java.time.OffsetDateTime;

public record RecentRegistrationDto(
        Long registrationId,
        String studentName,
        String className,
        String status,
        OffsetDateTime createdAt
) {
}
