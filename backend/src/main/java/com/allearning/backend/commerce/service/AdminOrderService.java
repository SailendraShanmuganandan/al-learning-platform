package com.allearning.backend.commerce.service;

import com.allearning.backend.commerce.dto.AdminOrderDetail;
import com.allearning.backend.commerce.dto.AdminOrderListItem;
import com.allearning.backend.commerce.dto.FulfillmentUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;

/**
 * Service contract for admin order management operations.
 */
public interface AdminOrderService {

    /**
     * Returns a paginated list of all orders with optional filters.
     *
     * @param paymentStatus     filter by order payment status (exact enum name, or null)
     * @param fulfillmentStatus filter by fulfillment status (exact enum name, or null)
     * @param q                 search term — exact order id or customer email (case-insensitive), or null
     * @param needsReview       when true, only orders with at least one payment needing review
     * @param from              optional created_at range start (inclusive), or null
     * @param to                optional created_at range end (inclusive), or null
     * @param pageable          pagination / sorting
     * @return page of AdminOrderListItem
     */
    Page<AdminOrderListItem> listOrders(
            String paymentStatus,
            String fulfillmentStatus,
            String q,
            Boolean needsReview,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable pageable
    );

    /**
     * Returns full order details including customer info, items, and all payments.
     *
     * @param orderId order ID
     * @return full detail DTO
     */
    AdminOrderDetail getOrderDetail(Long orderId);

    /**
     * Updates the fulfillment status of an order.
     * Enforces allowed transitions and locks the row to prevent race conditions.
     *
     * @param orderId   order ID
     * @param request   new status and optional tracking note
     * @param adminUserId admin's user ID (for logging)
     * @return updated full order detail
     */
    AdminOrderDetail updateFulfillmentStatus(Long orderId, FulfillmentUpdateRequest request, Long adminUserId);
}
