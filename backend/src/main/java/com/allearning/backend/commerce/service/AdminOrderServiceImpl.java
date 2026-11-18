package com.allearning.backend.commerce.service;

import com.allearning.backend.commerce.dto.*;
import com.allearning.backend.commerce.entity.*;
import com.allearning.backend.commerce.exception.OrderException;
import com.allearning.backend.commerce.repository.OrderRepository;
import com.allearning.backend.commerce.repository.PaymentRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class AdminOrderServiceImpl implements AdminOrderService {

    private static final Logger log = LoggerFactory.getLogger(AdminOrderServiceImpl.class);

    /** Allowed one-step forward transitions for fulfillment. */
    private static final Map<FulfillmentStatus, FulfillmentStatus> NEXT_STATUS = Map.of(
            FulfillmentStatus.PENDING, FulfillmentStatus.PROCESSING,
            FulfillmentStatus.PROCESSING, FulfillmentStatus.SHIPPED,
            FulfillmentStatus.SHIPPED, FulfillmentStatus.DELIVERED
    );

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final CustomerLookupService customerLookupService;

    public AdminOrderServiceImpl(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            CustomerLookupService customerLookupService
    ) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.customerLookupService = customerLookupService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // List orders
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<AdminOrderListItem> listOrders(
            String paymentStatus,
            String fulfillmentStatus,
            String q,
            Boolean needsReview,
            OffsetDateTime from,
            OffsetDateTime to,
            Pageable pageable
    ) {
        // Validate enum values early — return 400 on bad input
        PaymentStatus parsedPaymentStatus = parseEnum(PaymentStatus.class, paymentStatus, "paymentStatus");
        FulfillmentStatus parsedFulfillmentStatus = parseEnum(FulfillmentStatus.class, fulfillmentStatus, "fulfillmentStatus");

        // Resolve q: exact orderId or email → set of userIds
        Set<Long> emailUserIds = null;
        Long exactOrderId = null;

        if (q != null && !q.isBlank()) {
            String trimmed = q.trim();
            if (trimmed.contains("@")) {
                Long uid = customerLookupService.findUserIdByEmail(trimmed.toLowerCase());
                emailUserIds = uid != null ? Set.of(uid) : Set.of();
            } else {
                try {
                    exactOrderId = Long.parseLong(trimmed);
                } catch (NumberFormatException ex) {
                    return Page.empty(pageable);
                }
            }
        }

        final PaymentStatus finalPaymentStatus = parsedPaymentStatus;
        final FulfillmentStatus finalFulfillmentStatus = parsedFulfillmentStatus;
        final Set<Long> finalEmailUserIds = emailUserIds;
        final Long finalExactOrderId = exactOrderId;

        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (finalPaymentStatus != null) {
                predicates.add(cb.equal(root.get("paymentStatus"), finalPaymentStatus));
            }
            if (finalFulfillmentStatus != null) {
                predicates.add(cb.equal(root.get("fulfillmentStatus"), finalFulfillmentStatus));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            // q filter
            if (finalExactOrderId != null) {
                predicates.add(cb.equal(root.get("orderId"), finalExactOrderId));
            } else if (finalEmailUserIds != null) {
                if (finalEmailUserIds.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("userId").in(finalEmailUserIds));
                }
            }

            // needsReview filter: EXISTS (SELECT 1 FROM payments p WHERE p.orderId = o.orderId AND p.needsReview = true)
            if (Boolean.TRUE.equals(needsReview)) {
                Subquery<Long> sub = query.subquery(Long.class);
                var paymentRoot = sub.from(Payment.class);
                sub.select(paymentRoot.get("paymentId"))
                        .where(
                                cb.equal(paymentRoot.get("orderId"), root.get("orderId")),
                                cb.isTrue(paymentRoot.get("needsReview"))
                        );
                predicates.add(cb.exists(sub));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Order> orders = orderRepository.findAll(spec, pageable);

        // Bulk-load customer summaries
        Set<Long> userIds = new HashSet<>();
        orders.forEach(o -> userIds.add(o.getUserId()));
        Map<Long, CustomerSummaryDto> customers = customerLookupService.getCustomerSummaries(userIds);

        // Compute needsReview flag per order (batch)
        Set<Long> orderIds = new HashSet<>();
        orders.forEach(o -> orderIds.add(o.getOrderId()));
        Map<Long, Boolean> reviewFlags = computeNeedsReviewFlags(orderIds);

        return orders.map(o -> {
            CustomerSummaryDto cust = customers.get(o.getUserId());
            return new AdminOrderListItem(
                    o.getOrderId(),
                    cust != null ? cust.fullName() : "(unknown)",
                    cust != null ? cust.email() : "(unknown)",
                    o.getSubtotal(),
                    o.getDeliveryFee(),
                    o.getTotalAmount(),
                    o.getPaymentStatus(),
                    o.getFulfillmentStatus(),
                    o.getCreatedAt(),
                    reviewFlags.getOrDefault(o.getOrderId(), false)
            );
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Get order detail
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AdminOrderDetail getOrderDetail(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        Map<Long, CustomerSummaryDto> customers =
                customerLookupService.getCustomerSummaries(Set.of(order.getUserId()));
        CustomerSummaryDto cust = customers.get(order.getUserId());

        List<OrderItemResponse> items = order.getItems().stream()
                .map(i -> new OrderItemResponse(
                        i.getOrderItemId(), i.getProductId(), i.getProductName(),
                        i.getProductType(), i.getQuantity(), i.getUnitPrice(), i.getSubtotal()))
                .toList();

        List<PaymentDetailResponse> payments = paymentRepository
                .findByOrderIdOrderByCreatedAtDesc(orderId)
                .stream()
                .map(p -> new PaymentDetailResponse(
                        p.getPaymentId(), p.getOrderId(), p.getPaymentMethod(),
                        p.getPaymentStatus().name(), p.getAmount(), p.getTransactionReference(),
                        p.getPaidAt(), p.getCreatedAt(), p.isNeedsReview(),
                        p.getReviewReason(), p.getReviewNote(), p.getReviewedAt(), p.getRefundedAt()))
                .toList();

        return new AdminOrderDetail(
                order.getOrderId(),
                cust != null ? cust.fullName() : "(unknown)",
                cust != null ? cust.email() : "(unknown)",
                cust != null ? cust.phone() : null,
                order.getSubtotal(),
                order.getDeliveryFee(),
                order.getTotalAmount(),
                order.getPaymentStatus(),
                order.getFulfillmentStatus(),
                order.getTrackingNote(),
                order.getExpiresAt(),
                order.getCreatedAt(),
                order.getShipRecipientName(),
                order.getShipPhone(),
                order.getShipAddressLine1(),
                order.getShipAddressLine2(),
                order.getShipCity(),
                order.getShipDistrict(),
                order.getShipPostalCode(),
                items,
                payments
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Update fulfillment status
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AdminOrderDetail updateFulfillmentStatus(Long orderId, FulfillmentUpdateRequest request, Long adminUserId) {
        // Parse and validate target status
        FulfillmentStatus targetStatus;
        try {
            targetStatus = FulfillmentStatus.valueOf(request.status().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new OrderException(HttpStatus.BAD_REQUEST, "INVALID_STATUS",
                    "Unknown fulfillment status: " + request.status());
        }

        // Lock the order row for the duration of this transaction
        Order order = orderRepository.findByOrderIdWithLock(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        // Only PAID + non-NOT_REQUIRED orders are fulfillable
        if (order.getPaymentStatus() != PaymentStatus.PAID
                || order.getFulfillmentStatus() == FulfillmentStatus.NOT_REQUIRED) {
            throw new OrderException(HttpStatus.CONFLICT, "ORDER_NOT_FULFILLABLE",
                    "Order is not in a fulfillable state (must be PAID with a physical fulfillment flow).");
        }

        // Enforce one-step forward transitions
        FulfillmentStatus current = order.getFulfillmentStatus();
        FulfillmentStatus allowed = NEXT_STATUS.get(current);
        if (allowed == null || allowed != targetStatus) {
            throw new OrderException(HttpStatus.CONFLICT, "INVALID_TRANSITION",
                    "Cannot transition from " + current + " to " + targetStatus +
                    ". Next allowed step: " + (allowed != null ? allowed.name() : "none (already DELIVERED)."));
        }

        // trackingNote only accepted when moving to SHIPPED
        if (request.trackingNote() != null && !request.trackingNote().isBlank()) {
            if (targetStatus != FulfillmentStatus.SHIPPED) {
                throw new OrderException(HttpStatus.BAD_REQUEST, "TRACKING_NOTE_NOT_ALLOWED",
                        "trackingNote is only accepted when moving to SHIPPED.");
            }
            order.setTrackingNote(request.trackingNote().trim());
        }

        order.setFulfillmentStatus(targetStatus);
        orderRepository.save(order);

        log.info("Admin fulfillment update: adminUserId={}, orderId={}, newStatus={}",
                adminUserId, orderId, targetStatus);

        return getOrderDetail(orderId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String fieldName) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid " + fieldName + " value: " + value);
        }
    }

    /**
     * Efficient batch computation of needsReview per order ID.
     * Loads only the payments in scope (those belonging to the current page's order IDs).
     */
    private Map<Long, Boolean> computeNeedsReviewFlags(Set<Long> orderIds) {
        if (orderIds.isEmpty()) return Map.of();
        Map<Long, Boolean> result = new HashMap<>();
        orderIds.forEach(id -> result.put(id, false));
        List<Long> flagged = paymentRepository.findOrderIdsWithNeedsReview(orderIds);
        flagged.forEach(id -> result.put(id, true));
        return result;
    }
}
