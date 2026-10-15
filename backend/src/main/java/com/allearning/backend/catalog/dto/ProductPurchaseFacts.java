package com.allearning.backend.catalog.dto;

import com.allearning.backend.catalog.entity.ProductStatus;
import com.allearning.backend.catalog.entity.ProductType;

import java.math.BigDecimal;

/**
 * Internal projection containing purchase-relevant facts for a product.
 * Used across modular-monolith boundaries (e.g. by commerce for cart/checkout)
 * without leaking catalog entities or repositories.
 */
public record ProductPurchaseFacts(
        Long id,
        String name,
        String slug,
        ProductType type,
        ProductStatus status,
        BigDecimal currentPrice,
        String thumbnailUrl,
        Integer stockQuantity
) {
}
