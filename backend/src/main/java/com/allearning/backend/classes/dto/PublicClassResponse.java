package com.allearning.backend.classes.dto;

import com.allearning.backend.classes.entity.ClassType;
import java.math.BigDecimal;

public record PublicClassResponse(
        Long id,
        String name,
        String description,
        PublicSubjectRef subject,
        ClassType classType,
        String scheduleDescription,
        BigDecimal fee
) {}
