package com.allearning.backend.integration.payment;

import java.util.Map;

/**
 * Service contract for payment provider integration.
 * Enables swapping payment providers (e.g. PayHere, Stripe) without affecting the commerce domain.
 */
public interface PaymentGatewayProvider {

    /**
     * Checks if the payment provider is properly configured with credentials.
     */
    boolean isConfigured();

    /**
     * Builds the redirect checkout form URL and field values.
     *
     * @param orderDetails generic order details
     * @return checkout form response containing action URL and hidden fields
     */
    PaymentFormResponse createPaymentForm(PaymentOrderDetails orderDetails);

    /**
     * Verifies the signature of an incoming notification and parses generic payment outcome fields.
     *
     * @param params raw incoming form parameters
     * @return parsed notification result
     */
    PaymentNotificationResult verifyAndParseNotification(Map<String, String> params);
}
