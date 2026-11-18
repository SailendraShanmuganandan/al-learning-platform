package com.allearning.backend.classes.dto;

import com.allearning.backend.classes.entity.RegistrationStatus;
import com.allearning.backend.classes.service.StudentSummaryDto;
import java.time.OffsetDateTime;

public record AdminRegistrationResponse(
        Long registrationId,
        StudentSummaryDto student,
        Long classId,
        String className,
        RegistrationStatus status,
        OffsetDateTime registeredAt,
        OffsetDateTime updatedAt
) {}
