package com.allearning.backend.commerce.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CheckoutRequest(
        @JsonProperty("address_id")
        @JsonAlias({"addressId", "address_id"})
        Long addressId
) {
    @JsonProperty("addressId")
    public Long getAddressIdCamel() {
        return addressId;
    }
}
