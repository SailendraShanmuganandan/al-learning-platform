package com.allearning.backend.catalog.dto;

public record LowStockItemDto(
        Long productId,
        String name,
        Integer stock
) {
}
