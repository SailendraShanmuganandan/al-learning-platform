package com.allearning.backend.commerce.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaymentCreateRequest(
        @JsonProperty("orderId")
        Long orderIdCamel,
        @JsonProperty("order_id")
        Long orderIdSnake
) {
    public Long getOrderId() {
        return orderIdCamel != null ? orderIdCamel : orderIdSnake;
    }
}
