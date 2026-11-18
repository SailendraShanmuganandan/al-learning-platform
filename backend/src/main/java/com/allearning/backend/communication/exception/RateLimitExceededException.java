package com.allearning.backend.communication.exception;

public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;
    private final String errorCode;

    public RateLimitExceededException(long retryAfterSeconds, String message) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
        this.errorCode = "TOO_MANY_REQUESTS";
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
