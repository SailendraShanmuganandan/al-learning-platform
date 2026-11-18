package com.allearning.backend.commerce.service;

import com.allearning.backend.commerce.dto.PaymentCreateResponse;
import com.allearning.backend.commerce.dto.PaymentDetailResponse;

import java.util.Map;

/**
 * Service contract for payments in the commerce domain.
 */
public interface PaymentService {

    /**
     * Prepares and initiates a payment redirect checkout form for a student's pending order.
     *
     * @param currentUserId authenticated student user ID
     * @param orderId       order ID to pay for
     * @return checkout URL and form fields
     */
    PaymentCreateResponse createPayment(Long currentUserId, Long orderId);

    /**
     * Retrieves payment details by payment ID.
     * Accessible by the payment's owning student or an ADMIN.
     *
     * @param paymentId     payment ID
     * @param currentUserId current user ID
     * @param isAdmin       whether the caller is an administrator
     * @return payment details
     */
    PaymentDetailResponse getPayment(Long paymentId, Long currentUserId, boolean isAdmin);

    /**
     * Processes incoming webhook payment notifications.
     *
     * @param params form parameters sent by payment provider
     * @return HTTP status code to return (200 for valid signatures, 400 for bad signatures)
     */
    int processWebhookNotification(Map<String, String> params);
}
