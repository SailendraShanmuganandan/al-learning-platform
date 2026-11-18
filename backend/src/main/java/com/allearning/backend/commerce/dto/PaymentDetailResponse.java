package com.allearning.backend.commerce.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentDetailResponse(
        Long id,
        @JsonProperty("order_id")
        Long orderId,
        @JsonProperty("payment_method")
        String method,
        @JsonProperty("payment_status")
        String status,
        BigDecimal amount,
        @JsonProperty("transaction_reference")
        String transactionReference,
        @JsonProperty("paid_at")
        OffsetDateTime paidAt,
        @JsonProperty("created_at")
        OffsetDateTime createdAt,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("needs_review")
        Boolean needsReview,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("review_reason")
        String reviewReason,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("review_note")
        String reviewNote,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("reviewed_at")
        OffsetDateTime reviewedAt,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("refunded_at")
        OffsetDateTime refundedAt
) {
    /** Backward-compat constructor used by existing PaymentServiceImpl.getPayment */
    public PaymentDetailResponse(Long id, Long orderId, String method, String status,
                                 BigDecimal amount, OffsetDateTime paidAt, OffsetDateTime createdAt,
                                 Boolean needsReview, String reviewReason) {
        this(id, orderId, method, status, amount, null, paidAt, createdAt,
                needsReview, reviewReason, null, null, null);
    }

    @JsonProperty("orderId")
    public Long getOrderIdCamel() { return orderId; }

    @JsonProperty("method")
    public String getMethodCamel() { return method; }

    @JsonProperty("status")
    public String getStatusCamel() { return status; }

    @JsonProperty("transactionReference")
    public String getTransactionReferenceCamel() { return transactionReference; }

    @JsonProperty("paidAt")
    public OffsetDateTime getPaidAtCamel() { return paidAt; }

    @JsonProperty("createdAt")
    public OffsetDateTime getCreatedAtCamel() { return createdAt; }

    @JsonProperty("needsReview")
    public Boolean getNeedsReviewCamel() { return needsReview; }

    @JsonProperty("reviewReason")
    public String getReviewReasonCamel() { return reviewReason; }

    @JsonProperty("reviewNote")
    public String getReviewNoteCamel() { return reviewNote; }

    @JsonProperty("reviewedAt")
    public OffsetDateTime getReviewedAtCamel() { return reviewedAt; }

    @JsonProperty("refundedAt")
    public OffsetDateTime getRefundedAtCamel() { return refundedAt; }
}
