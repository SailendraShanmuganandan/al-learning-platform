package com.allearning.backend.commerce.dto;

import com.allearning.backend.commerce.entity.FulfillmentStatus;
import com.allearning.backend.commerce.entity.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record OrderResponse(
        @JsonProperty("order_id")
        Long orderId,
        @JsonProperty("user_id")
        Long userId,
        @JsonProperty("payment_status")
        PaymentStatus paymentStatus,
        @JsonProperty("fulfillment_status")
        FulfillmentStatus fulfillmentStatus,
        BigDecimal subtotal,
        @JsonProperty("delivery_fee")
        BigDecimal deliveryFee,
        @JsonProperty("total_amount")
        BigDecimal totalAmount,
        @JsonProperty("expires_at")
        OffsetDateTime expiresAt,
        @JsonProperty("created_at")
        OffsetDateTime createdAt,
        @JsonProperty("updated_at")
        OffsetDateTime updatedAt,
        @JsonProperty("ship_recipient_name")
        String shipRecipientName,
        @JsonProperty("ship_phone")
        String shipPhone,
        @JsonProperty("ship_address_line_1")
        String shipAddressLine1,
        @JsonProperty("ship_address_line_2")
        String shipAddressLine2,
        @JsonProperty("ship_city")
        String shipCity,
        @JsonProperty("ship_district")
        String shipDistrict,
        @JsonProperty("ship_postal_code")
        String shipPostalCode,
        List<OrderItemResponse> items
) {
    @JsonProperty("orderId")
    public Long getOrderIdCamel() {
        return orderId;
    }

    @JsonProperty("userId")
    public Long getUserIdCamel() {
        return userId;
    }

    @JsonProperty("paymentStatus")
    public PaymentStatus getPaymentStatusCamel() {
        return paymentStatus;
    }

    @JsonProperty("fulfillmentStatus")
    public FulfillmentStatus getFulfillmentStatusCamel() {
        return fulfillmentStatus;
    }

    @JsonProperty("deliveryFee")
    public BigDecimal getDeliveryFeeCamel() {
        return deliveryFee;
    }

    @JsonProperty("totalAmount")
    public BigDecimal getTotalAmountCamel() {
        return totalAmount;
    }

    @JsonProperty("expiresAt")
    public OffsetDateTime getExpiresAtCamel() {
        return expiresAt;
    }

    @JsonProperty("createdAt")
    public OffsetDateTime getCreatedAtCamel() {
        return createdAt;
    }

    @JsonProperty("updatedAt")
    public OffsetDateTime getUpdatedAtCamel() {
        return updatedAt;
    }

    @JsonProperty("shipRecipientName")
    public String getShipRecipientNameCamel() {
        return shipRecipientName;
    }

    @JsonProperty("shipPhone")
    public String getShipPhoneCamel() {
        return shipPhone;
    }

    @JsonProperty("shipAddressLine1")
    public String getShipAddressLine1Camel() {
        return shipAddressLine1;
    }

    @JsonProperty("shipAddressLine2")
    public String getShipAddressLine2Camel() {
        return shipAddressLine2;
    }

    @JsonProperty("shipCity")
    public String getShipCityCamel() {
        return shipCity;
    }

    @JsonProperty("shipDistrict")
    public String getShipDistrictCamel() {
        return shipDistrict;
    }

    @JsonProperty("shipPostalCode")
    public String getShipPostalCodeCamel() {
        return shipPostalCode;
    }
}
