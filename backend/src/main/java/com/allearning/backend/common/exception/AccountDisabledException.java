package com.allearning.backend.common.exception;

/**
 * Thrown when an account is not active (e.g. SUSPENDED or DELETED) during login attempt.
 */
public class AccountDisabledException extends RuntimeException {

    public AccountDisabledException(String message) {
        super(message);
    }
}
