package com.allearning.backend.classes.dto;

import com.allearning.backend.classes.entity.ClassType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

public record UpdateClassRequest(
        String name,
        String description,
        ClassType classType,
        String scheduleDescription,
        @DecimalMin(value = "0.00", message = "Fee must be greater than or equal to 0")
        @Digits(integer = 10, fraction = 2, message = "Fee can have at most 2 decimal places")
        BigDecimal fee
) {}
