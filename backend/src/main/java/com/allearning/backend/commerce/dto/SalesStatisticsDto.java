package com.allearning.backend.commerce.dto;

import java.math.BigDecimal;

public record SalesStatisticsDto(
        BigDecimal revenueToday,
        BigDecimal revenueLast7Days,
        BigDecimal revenueLast30Days,
        BigDecimal revenueAllTime,
        long paidOrdersCount,
        long awaitingPaymentCount
) {
}
