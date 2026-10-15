package com.allearning.backend.commerce.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public record CartResponse(
        @JsonProperty("cart_id")
        Long cartId,
        List<CartItemResponse> items,
        @JsonProperty("item_count")
        int itemCount,
        BigDecimal subtotal
) {
    public static CartResponse empty() {
        return new CartResponse(null, Collections.emptyList(), 0, BigDecimal.ZERO.setScale(2));
    }

    @JsonProperty("cartId")
    public Long getCartIdCamel() {
        return cartId;
    }

    @JsonProperty("itemCount")
    public int getItemCountCamel() {
        return itemCount;
    }
}
