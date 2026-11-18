package com.allearning.backend.commerce.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record RecentOrderDto(
        Long orderId,
        String customerName,
        BigDecimal total,
        String paymentStatus,
        OffsetDateTime createdAt
) {
}
