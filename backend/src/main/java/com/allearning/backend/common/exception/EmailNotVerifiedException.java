package com.allearning.backend.common.exception;

/**
 * Thrown when an action requires a verified email address, but the user's
 * {@code is_email_verified} flag is false (SRS FR-AUTH-14).
 */
public class EmailNotVerifiedException extends RuntimeException {

    private final String errorCode;

    public EmailNotVerifiedException() {
        this("EMAIL_NOT_VERIFIED", "Email verification is required to perform this action.");
    }

    public EmailNotVerifiedException(String message) {
        this("EMAIL_NOT_VERIFIED", message);
    }

    public EmailNotVerifiedException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
