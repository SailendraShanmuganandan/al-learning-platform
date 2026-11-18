package com.allearning.backend.learning.exception;

public class LearningConflictException extends RuntimeException {
    private final String errorCode;

    public LearningConflictException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public LearningConflictException(String message) {
        super(message);
        this.errorCode = "CONFLICT";
    }

    public String getErrorCode() {
        return errorCode;
    }
}

