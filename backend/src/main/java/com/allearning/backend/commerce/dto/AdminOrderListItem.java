package com.allearning.backend.commerce.dto;

import com.allearning.backend.commerce.entity.FulfillmentStatus;
import com.allearning.backend.commerce.entity.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Row DTO for the admin order list endpoint.
 */
public record AdminOrderListItem(
        @JsonProperty("order_id")
        Long orderId,
        @JsonProperty("customer_name")
        String customerName,
        @JsonProperty("customer_email")
        String customerEmail,
        BigDecimal subtotal,
        @JsonProperty("delivery_fee")
        BigDecimal deliveryFee,
        @JsonProperty("total_amount")
        BigDecimal totalAmount,
        @JsonProperty("payment_status")
        PaymentStatus paymentStatus,
        @JsonProperty("fulfillment_status")
        FulfillmentStatus fulfillmentStatus,
        @JsonProperty("created_at")
        OffsetDateTime createdAt,
        @JsonProperty("needs_review")
        boolean needsReview
) {
    @JsonProperty("orderId")
    public Long getOrderIdCamel() { return orderId; }
    @JsonProperty("customerName")
    public String getCustomerNameCamel() { return customerName; }
    @JsonProperty("customerEmail")
    public String getCustomerEmailCamel() { return customerEmail; }
    @JsonProperty("deliveryFee")
    public BigDecimal getDeliveryFeeCamel() { return deliveryFee; }
    @JsonProperty("totalAmount")
    public BigDecimal getTotalAmountCamel() { return totalAmount; }
    @JsonProperty("paymentStatus")
    public PaymentStatus getPaymentStatusCamel() { return paymentStatus; }
    @JsonProperty("fulfillmentStatus")
    public FulfillmentStatus getFulfillmentStatusCamel() { return fulfillmentStatus; }
    @JsonProperty("createdAt")
    public OffsetDateTime getCreatedAtCamel() { return createdAt; }
    @JsonProperty("needsReview")
    public boolean getNeedsReviewCamel() { return needsReview; }
}
