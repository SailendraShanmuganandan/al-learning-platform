package com.allearning.backend.communication.exception;

public class InvalidTransitionException extends RuntimeException {

    private final String errorCode;

    public InvalidTransitionException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
