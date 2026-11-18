package com.allearning.backend.commerce.dto;

import com.allearning.backend.commerce.entity.FulfillmentStatus;
import com.allearning.backend.commerce.entity.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Full detail DTO for the admin order detail endpoint.
 */
public record AdminOrderDetail(
        @JsonProperty("order_id")
        Long orderId,
        @JsonProperty("customer_name")
        String customerName,
        @JsonProperty("customer_email")
        String customerEmail,
        @JsonProperty("customer_phone")
        String customerPhone,
        BigDecimal subtotal,
        @JsonProperty("delivery_fee")
        BigDecimal deliveryFee,
        @JsonProperty("total_amount")
        BigDecimal totalAmount,
        @JsonProperty("payment_status")
        PaymentStatus paymentStatus,
        @JsonProperty("fulfillment_status")
        FulfillmentStatus fulfillmentStatus,
        @JsonProperty("tracking_note")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String trackingNote,
        @JsonProperty("expires_at")
        OffsetDateTime expiresAt,
        @JsonProperty("created_at")
        OffsetDateTime createdAt,
        @JsonProperty("ship_recipient_name")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String shipRecipientName,
        @JsonProperty("ship_phone")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String shipPhone,
        @JsonProperty("ship_address_line_1")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String shipAddressLine1,
        @JsonProperty("ship_address_line_2")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String shipAddressLine2,
        @JsonProperty("ship_city")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String shipCity,
        @JsonProperty("ship_district")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String shipDistrict,
        @JsonProperty("ship_postal_code")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String shipPostalCode,
        List<OrderItemResponse> items,
        List<PaymentDetailResponse> payments
) {
    @JsonProperty("orderId") public Long getOrderIdCamel() { return orderId; }
    @JsonProperty("customerName") public String getCustomerNameCamel() { return customerName; }
    @JsonProperty("customerEmail") public String getCustomerEmailCamel() { return customerEmail; }
    @JsonProperty("customerPhone") public String getCustomerPhoneCamel() { return customerPhone; }
    @JsonProperty("deliveryFee") public BigDecimal getDeliveryFeeCamel() { return deliveryFee; }
    @JsonProperty("totalAmount") public BigDecimal getTotalAmountCamel() { return totalAmount; }
    @JsonProperty("paymentStatus") public PaymentStatus getPaymentStatusCamel() { return paymentStatus; }
    @JsonProperty("fulfillmentStatus") public FulfillmentStatus getFulfillmentStatusCamel() { return fulfillmentStatus; }
    @JsonProperty("trackingNote") public String getTrackingNoteCamel() { return trackingNote; }
    @JsonProperty("expiresAt") public OffsetDateTime getExpiresAtCamel() { return expiresAt; }
    @JsonProperty("createdAt") public OffsetDateTime getCreatedAtCamel() { return createdAt; }
    @JsonProperty("shipRecipientName") public String getShipRecipientNameCamel() { return shipRecipientName; }
    @JsonProperty("shipPhone") public String getShipPhoneCamel() { return shipPhone; }
    @JsonProperty("shipAddressLine1") public String getShipAddressLine1Camel() { return shipAddressLine1; }
    @JsonProperty("shipAddressLine2") public String getShipAddressLine2Camel() { return shipAddressLine2; }
    @JsonProperty("shipCity") public String getShipCityCamel() { return shipCity; }
    @JsonProperty("shipDistrict") public String getShipDistrictCamel() { return shipDistrict; }
    @JsonProperty("shipPostalCode") public String getShipPostalCodeCamel() { return shipPostalCode; }
}
