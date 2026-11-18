package com.allearning.backend.commerce.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Row DTO for the admin payments list endpoint.
 */
public record AdminPaymentListItem(
        @JsonProperty("payment_id")
        Long paymentId,
        @JsonProperty("order_id")
        Long orderId,
        BigDecimal amount,
        @JsonProperty("payment_status")
        String paymentStatus,
        @JsonProperty("transaction_reference")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String transactionReference,
        @JsonProperty("needs_review")
        boolean needsReview,
        @JsonProperty("review_reason")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String reviewReason,
        @JsonProperty("paid_at")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        OffsetDateTime paidAt,
        @JsonProperty("created_at")
        OffsetDateTime createdAt
) {
    @JsonProperty("paymentId") public Long getPaymentIdCamel() { return paymentId; }
    @JsonProperty("orderId") public Long getOrderIdCamel() { return orderId; }
    @JsonProperty("paymentStatus") public String getPaymentStatusCamel() { return paymentStatus; }
    @JsonProperty("transactionReference") public String getTransactionReferenceCamel() { return transactionReference; }
    @JsonProperty("needsReview") public boolean getNeedsReviewCamel() { return needsReview; }
    @JsonProperty("reviewReason") public String getReviewReasonCamel() { return reviewReason; }
    @JsonProperty("paidAt") public OffsetDateTime getPaidAtCamel() { return paidAt; }
    @JsonProperty("createdAt") public OffsetDateTime getCreatedAtCamel() { return createdAt; }
}
