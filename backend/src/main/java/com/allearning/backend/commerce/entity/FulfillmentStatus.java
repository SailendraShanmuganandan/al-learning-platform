package com.allearning.backend.commerce.entity;

/**
 * Fulfillment status lifecycle for orders.
 */
public enum FulfillmentStatus {
    NOT_REQUIRED,
    PENDING,
    PROCESSING,
    SHIPPED,
    DELIVERED
}
