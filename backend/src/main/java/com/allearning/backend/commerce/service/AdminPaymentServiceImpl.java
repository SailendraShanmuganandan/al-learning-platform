package com.allearning.backend.commerce.service;

import com.allearning.backend.commerce.dto.AdminPaymentListItem;
import com.allearning.backend.commerce.dto.PaymentDetailResponse;
import com.allearning.backend.commerce.dto.PaymentReviewRequest;
import com.allearning.backend.commerce.entity.*;
import com.allearning.backend.commerce.exception.PaymentException;
import com.allearning.backend.commerce.repository.OrderRepository;
import com.allearning.backend.commerce.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.criteria.Predicate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminPaymentServiceImpl implements AdminPaymentService {

    private static final Logger log = LoggerFactory.getLogger(AdminPaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public AdminPaymentServiceImpl(PaymentRepository paymentRepository, OrderRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // List payments
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<AdminPaymentListItem> listPayments(String status, Boolean needsReview, Long orderId, Pageable pageable) {
        PaymentRecordStatus parsedStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                parsedStatus = PaymentRecordStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid status value: " + status);
            }
        }

        final PaymentRecordStatus finalStatus = parsedStatus;

        Specification<Payment> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (finalStatus != null) {
                predicates.add(cb.equal(root.get("paymentStatus"), finalStatus));
            }
            if (Boolean.TRUE.equals(needsReview)) {
                predicates.add(cb.isTrue(root.get("needsReview")));
            }
            if (orderId != null) {
                predicates.add(cb.equal(root.get("orderId"), orderId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Payment> paymentPage = paymentRepository.findAll(spec, pageable);

        List<AdminPaymentListItem> items = paymentPage.getContent().stream()
                .map(p -> new AdminPaymentListItem(
                        p.getPaymentId(),
                        p.getOrderId(),
                        p.getAmount(),
                        p.getPaymentStatus().name(),
                        p.getTransactionReference(),
                        p.isNeedsReview(),
                        p.getReviewReason(),
                        p.getPaidAt(),
                        p.getCreatedAt()
                ))
                .toList();

        return new PageImpl<>(items, pageable, paymentPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public long countNeedsReview() {
        return paymentRepository.countByNeedsReviewTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Resolve review
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentDetailResponse resolveReview(Long paymentId, PaymentReviewRequest request, Long adminUserId) {
        if (request.note() == null || request.note().isBlank()) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "NOTE_REQUIRED", "A note is required.");
        }

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found."));

        if (!payment.isNeedsReview()) {
            throw new PaymentException(HttpStatus.CONFLICT, "PAYMENT_NOT_FLAGGED",
                    "Payment is not flagged for review.");
        }

        payment.setNeedsReview(false);
        payment.setReviewNote(request.note().trim());
        payment.setReviewedBy(adminUserId);
        payment.setReviewedAt(OffsetDateTime.now());
        paymentRepository.save(payment);

        log.info("Admin resolved review: adminUserId={}, paymentId={}", adminUserId, paymentId);

        return mapToDetailResponse(payment);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Record refund
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PaymentDetailResponse recordRefund(Long paymentId, PaymentReviewRequest request, Long adminUserId) {
        if (request.note() == null || request.note().isBlank()) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "NOTE_REQUIRED", "A note is required.");
        }

        // Lock the payment row
        Payment payment = paymentRepository.findByIdWithLock(paymentId)
                .orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment not found."));

        if (payment.getPaymentStatus() != PaymentRecordStatus.PAID) {
            throw new PaymentException(HttpStatus.CONFLICT, "PAYMENT_NOT_REFUNDABLE",
                    "Only PAID payments can be marked as refunded.");
        }

        // Lock the order too (consistent ordering: always lock order before payment in other flows;
        // here we already hold payment lock, so fetch order non-locking to avoid deadlock risk)
        Order order = orderRepository.findByOrderIdWithLock(payment.getOrderId())
                .orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "Order not found."));

        OffsetDateTime now = OffsetDateTime.now();

        // Mark payment as REFUNDED
        payment.setPaymentStatus(PaymentRecordStatus.REFUNDED);
        payment.setRefundedAt(now);
        payment.setReviewNote(request.note().trim());
        payment.setReviewedBy(adminUserId);
        payment.setReviewedAt(now);
        payment.setNeedsReview(false);
        paymentRepository.save(payment);

        // If the order is PAID, mark it REFUNDED (revokes video ownership automatically)
        // If the order is EXPIRED (late payment case), leave it EXPIRED.
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            orderRepository.save(order);
            log.info("Admin refund recorded: adminUserId={}, paymentId={}, orderId={} -> order now REFUNDED",
                    adminUserId, paymentId, order.getOrderId());
        } else {
            log.info("Admin refund recorded: adminUserId={}, paymentId={}, orderId={} left as {}",
                    adminUserId, paymentId, order.getOrderId(), order.getPaymentStatus());
        }

        return mapToDetailResponse(payment);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    private PaymentDetailResponse mapToDetailResponse(Payment p) {
        return new PaymentDetailResponse(
                p.getPaymentId(), p.getOrderId(), p.getPaymentMethod(),
                p.getPaymentStatus().name(), p.getAmount(), p.getTransactionReference(),
                p.getPaidAt(), p.getCreatedAt(), p.isNeedsReview(),
                p.getReviewReason(), p.getReviewNote(), p.getReviewedAt(), p.getRefundedAt()
        );
    }
}
