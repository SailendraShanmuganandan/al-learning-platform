package com.allearning.backend.classes.exception;

import java.util.Map;

public class ClassRegistrationConflictException extends RuntimeException {

    private final String errorCode;
    private final Map<String, Object> details;

    public ClassRegistrationConflictException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }

    public ClassRegistrationConflictException(String errorCode, String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
