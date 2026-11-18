package com.allearning.backend.commerce.exception;

import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Map;

/**
 * Exception thrown for payment domain errors.
 */
public class PaymentException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final Map<String, Object> details;

    public PaymentException(HttpStatus status, String errorCode, String message) {
        this(status, errorCode, message, Collections.emptyMap());
    }

    public PaymentException(HttpStatus status, String errorCode, String message, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.details = details != null ? details : Collections.emptyMap();
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
