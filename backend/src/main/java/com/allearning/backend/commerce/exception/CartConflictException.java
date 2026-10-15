package com.allearning.backend.commerce.exception;

/**
 * Thrown when a shopping cart operation causes a conflict (HTTP 409).
 * Holds a machine-readable code (e.g. OUT_OF_STOCK, INSUFFICIENT_STOCK,
 * ALREADY_IN_CART, VIDEO_ALREADY_OWNED).
 */
public class CartConflictException extends RuntimeException {

    private final String errorCode;

    public CartConflictException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
