package com.allearning.backend.commerce.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record OrderItemResponse(
        @JsonProperty("order_item_id")
        Long orderItemId,
        @JsonProperty("product_id")
        Long productId,
        @JsonProperty("product_name")
        String productName,
        @JsonProperty("product_type")
        String productType,
        int quantity,
        @JsonProperty("unit_price")
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
    @JsonProperty("orderItemId")
    public Long getOrderItemIdCamel() {
        return orderItemId;
    }

    @JsonProperty("productId")
    public Long getProductIdCamel() {
        return productId;
    }

    @JsonProperty("productName")
    public String getProductNameCamel() {
        return productName;
    }

    @JsonProperty("productType")
    public String getProductTypeCamel() {
        return productType;
    }

    @JsonProperty("unitPrice")
    public BigDecimal getUnitPriceCamel() {
        return unitPrice;
    }
}
