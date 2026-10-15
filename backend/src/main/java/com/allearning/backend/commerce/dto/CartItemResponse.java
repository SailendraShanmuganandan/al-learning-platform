package com.allearning.backend.commerce.dto;

import com.allearning.backend.catalog.entity.ProductType;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        @JsonProperty("product_id")
        Long productId,
        String name,
        String slug,
        ProductType type,
        @JsonProperty("thumbnail_url")
        String thumbnailUrl,
        Integer quantity,
        @JsonProperty("unit_price")
        BigDecimal unitPrice,
        @JsonProperty("line_total")
        BigDecimal lineTotal,
        @JsonProperty("current_price")
        BigDecimal currentPrice,
        @JsonProperty("price_changed")
        boolean priceChanged,
        boolean unavailable,
        @JsonProperty("out_of_stock")
        boolean outOfStock,
        @JsonProperty("insufficient_stock")
        boolean insufficientStock
) {
    @JsonProperty("item_id")
    public Long getItemId() {
        return id;
    }

    @JsonProperty("productId")
    public Long getProductIdCamel() {
        return productId;
    }

    @JsonProperty("thumbnailUrl")
    public String getThumbnailUrlCamel() {
        return thumbnailUrl;
    }

    @JsonProperty("unitPrice")
    public BigDecimal getUnitPriceCamel() {
        return unitPrice;
    }

    @JsonProperty("lineTotal")
    public BigDecimal getLineTotalCamel() {
        return lineTotal;
    }

    @JsonProperty("currentPrice")
    public BigDecimal getCurrentPriceCamel() {
        return currentPrice;
    }

    @JsonProperty("priceChanged")
    public boolean isPriceChangedCamel() {
        return priceChanged;
    }

    @JsonProperty("outOfStock")
    public boolean isOutOfStockCamel() {
        return outOfStock;
    }

    @JsonProperty("insufficientStock")
    public boolean isInsufficientStockCamel() {
        return insufficientStock;
    }
}
