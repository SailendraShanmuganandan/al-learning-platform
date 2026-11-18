package com.allearning.backend.commerce.dto;

import java.math.BigDecimal;
import java.util.List;

public record RevenueSeriesDto(
        List<DailyRevenueEntryDto> entries,
        BigDecimal totalRevenue,
        long totalPaidOrders
) {
}
