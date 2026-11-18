package com.allearning.backend.integration.payment;

import java.math.BigDecimal;

/**
 * Generic result of parsing and verifying an incoming webhook payment notification.
 * Contains no provider-specific parameter names.
 */
public record PaymentNotificationResult(
        boolean signatureValid,
        boolean merchantIdMatches,
        String orderId,
        String transactionReference,
        BigDecimal amount,
        String currency,
        Integer statusCode,
        String statusMessage,
        String paymentMethod
) {
}
