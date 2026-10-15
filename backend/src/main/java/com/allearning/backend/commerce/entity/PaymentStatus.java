package com.allearning.backend.commerce.entity;

/**
 * Payment status lifecycle for orders.
 */
public enum PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    REFUNDED,
    EXPIRED
}
