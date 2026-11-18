package com.allearning.backend.commerce.service;

import com.allearning.backend.commerce.dto.AdminPaymentListItem;
import com.allearning.backend.commerce.dto.PaymentReviewRequest;
import com.allearning.backend.commerce.dto.PaymentDetailResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service contract for admin payment review and refund operations.
 */
public interface AdminPaymentService {

    /**
     * Returns a paginated list of payments with optional filters.
     *
     * @param status      filter by payment record status (PENDING/PAID/FAILED/REFUNDED), or null
     * @param needsReview if true, only payments flagged for review
     * @param orderId     filter by a specific order ID, or null
     * @param pageable    pagination/sorting
     * @return page of AdminPaymentListItem
     */
    Page<AdminPaymentListItem> listPayments(String status, Boolean needsReview, Long orderId, Pageable pageable);

    /**
     * Returns the count of payments currently flagged as needs_review.
     * Used for the sidebar badge.
     */
    long countNeedsReview();

    /**
     * Marks a payment's review as resolved: clears needs_review, stores the note.
     * Only allowed if the payment currently has needs_review = true.
     *
     * @param paymentId   payment ID
     * @param request     note body (required)
     * @param adminUserId admin's user ID
     * @return updated payment detail
     */
    PaymentDetailResponse resolveReview(Long paymentId, PaymentReviewRequest request, Long adminUserId);

    /**
     * Records that a refund was already issued in the payment provider dashboard.
     * Sets the payment to REFUNDED and potentially the order to REFUNDED.
     * Only allowed on a PAID payment.
     *
     * @param paymentId   payment ID
     * @param request     note body (required)
     * @param adminUserId admin's user ID
     * @return updated payment detail
     */
    PaymentDetailResponse recordRefund(Long paymentId, PaymentReviewRequest request, Long adminUserId);
}
