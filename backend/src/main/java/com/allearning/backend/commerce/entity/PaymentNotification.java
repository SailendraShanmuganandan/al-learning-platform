package com.allearning.backend.commerce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * JPA entity for payment notification audit logging.
 * Does not store any card data or raw request bodies.
 */
@Entity
@Table(name = "payment_notifications")
public class PaymentNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "payhere_payment_id")
    private String payherePaymentId;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "signature_valid", nullable = false)
    private boolean signatureValid;

    @Column(name = "outcome", nullable = false, length = 100)
    private String outcome;

    @Column(name = "received_at", nullable = false)
    private OffsetDateTime receivedAt;

    public PaymentNotification() {
    }

    public PaymentNotification(Long orderId,
                               String payherePaymentId,
                               Integer statusCode,
                               BigDecimal amount,
                               String currency,
                               boolean signatureValid,
                               String outcome,
                               OffsetDateTime receivedAt) {
        this.orderId = orderId;
        this.payherePaymentId = payherePaymentId;
        this.statusCode = statusCode;
        this.amount = amount;
        this.currency = currency;
        this.signatureValid = signatureValid;
        this.outcome = outcome;
        this.receivedAt = receivedAt != null ? receivedAt : OffsetDateTime.now();
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getPayherePaymentId() {
        return payherePaymentId;
    }

    public void setPayherePaymentId(String payherePaymentId) {
        this.payherePaymentId = payherePaymentId;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public boolean isSignatureValid() {
        return signatureValid;
    }

    public void setSignatureValid(boolean signatureValid) {
        this.signatureValid = signatureValid;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public OffsetDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(OffsetDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }
}
