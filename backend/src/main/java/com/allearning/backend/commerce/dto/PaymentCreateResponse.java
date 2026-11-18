package com.allearning.backend.commerce.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record PaymentCreateResponse(
        @JsonProperty("checkoutUrl")
        String checkoutUrlCamel,
        @JsonProperty("checkout_url")
        String checkoutUrlSnake,
        Map<String, String> fields
) {
    public PaymentCreateResponse(String checkoutUrl, Map<String, String> fields) {
        this(checkoutUrl, checkoutUrl, fields);
    }
}
