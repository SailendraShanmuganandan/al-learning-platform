package com.allearning.backend.integration.payment;

import java.util.Map;

/**
 * Generic response from payment gateway provider for redirect form submission.
 */
public record PaymentFormResponse(
        String checkoutUrl,
        Map<String, String> fields
) {
}
