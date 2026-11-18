package com.allearning.backend.commerce.entity;

/**
 * Status of a payment transaction record.
 */
public enum PaymentRecordStatus {
    PENDING,
    PAID,
    FAILED,
    REFUNDED
}
