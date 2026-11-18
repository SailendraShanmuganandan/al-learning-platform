package com.allearning.backend.commerce.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyRevenueEntryDto(
        LocalDate date,
        BigDecimal revenue,
        long paidOrderCount
) {
}
