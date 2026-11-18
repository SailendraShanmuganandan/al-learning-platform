package com.allearning.backend.classes.dto;

import com.allearning.backend.classes.entity.RegistrationStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record StudentRegistrationResponse(
        Long registrationId,
        Long classId,
        String className,
        String subjectName,
        String scheduleDescription,
        BigDecimal fee,
        RegistrationStatus status,
        OffsetDateTime registeredAt,
        OffsetDateTime updatedAt
) {}
