package com.allearning.backend.classes.dto;

import com.allearning.backend.classes.entity.ClassType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreateClassRequest(
        @NotNull(message = "Subject ID is required")
        Long subjectId,

        @NotBlank(message = "Name is required")
        String name,

        String description,

        @NotNull(message = "Class type is required")
        ClassType classType,

        @NotBlank(message = "Schedule description is required")
        String scheduleDescription,

        @NotNull(message = "Fee is required")
        @DecimalMin(value = "0.00", message = "Fee must be greater than or equal to 0")
        @Digits(integer = 10, fraction = 2, message = "Fee can have at most 2 decimal places")
        BigDecimal fee
) {}
