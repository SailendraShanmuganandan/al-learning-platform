package com.allearning.backend.commerce.dto;

import java.math.BigDecimal;

public record TopProductDto(
        Long productId,
        String name,
        String type,
        long quantitySold,
        BigDecimal revenue
) {
}
