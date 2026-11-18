package com.allearning.backend.learning.exception;

/**
 * Thrown when a student attempts to report a question they have never answered in a completed attempt (SEC-21).
 */
public class NotAnsweredByUserException extends RuntimeException {

    private final String errorCode;

    public NotAnsweredByUserException(String message) {
        super(message);
        this.errorCode = "NOT_ANSWERED_BY_USER";
    }

    public NotAnsweredByUserException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

