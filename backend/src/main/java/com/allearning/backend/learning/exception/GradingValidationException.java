package com.allearning.backend.learning.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class GradingValidationException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;
    private final Map<String, Object> details;

    public GradingValidationException(String errorCode, String message) {
        this(HttpStatus.BAD_REQUEST, errorCode, message, Map.of());
    }

    public GradingValidationException(HttpStatus status, String errorCode, String message, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.details = details != null ? details : Map.of();
    }

    public HttpStatus getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
    public Map<String, Object> getDetails() { return details; }
}

