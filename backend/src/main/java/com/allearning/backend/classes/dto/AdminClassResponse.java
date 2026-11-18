package com.allearning.backend.classes.dto;

import com.allearning.backend.classes.entity.ClassStatus;
import com.allearning.backend.classes.entity.ClassType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdminClassResponse(
        Long classId,
        Long subjectId,
        String subjectName,
        String name,
        String description,
        ClassType classType,
        String scheduleDescription,
        BigDecimal fee,
        ClassStatus status,
        long pendingCount,
        long confirmedCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
