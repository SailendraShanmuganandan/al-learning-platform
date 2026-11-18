package com.allearning.backend.integration.payment;

import java.math.BigDecimal;

/**
 * Generic order details required to build a payment redirect form.
 * Contains no provider-specific field names.
 */
public record PaymentOrderDetails(
        Long orderId,
        BigDecimal amount,
        String currency,
        String customerFirstName,
        String customerLastName,
        String customerEmail,
        String customerPhone,
        String deliveryAddress,
        String deliveryCity,
        String deliveryCountry,
        String itemsDescription
) {
}
