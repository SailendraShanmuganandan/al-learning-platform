package com.allearning.backend.commerce.controller;

import com.allearning.backend.commerce.dto.AdminPaymentListItem;
import com.allearning.backend.commerce.dto.PaymentDetailResponse;
import com.allearning.backend.commerce.dto.PaymentReviewRequest;
import com.allearning.backend.commerce.service.AdminPaymentService;
import com.allearning.backend.commerce.service.CustomerLookupService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Controller for admin payment review and refund operations.
 * All endpoints require ROLE_ADMIN (enforced by SecurityConfig).
 */
@RestController
@RequestMapping("/api/admin/payments")
public class AdminPaymentController {

    private static final Logger log = LoggerFactory.getLogger(AdminPaymentController.class);

    private final AdminPaymentService adminPaymentService;
    private final CustomerLookupService customerLookupService;

    public AdminPaymentController(
            AdminPaymentService adminPaymentService,
            CustomerLookupService customerLookupService
    ) {
        this.adminPaymentService = adminPaymentService;
        this.customerLookupService = customerLookupService;
    }

    /**
     * GET /api/admin/payments
     * Returns a paginated list of payments with optional filters.
     */
    @GetMapping
    public ResponseEntity<Page<AdminPaymentListItem>> getPayments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean needsReview,
            @RequestParam(required = false) Long orderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AdminPaymentListItem> result = adminPaymentService.listPayments(status, needsReview, orderId, pageRequest);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/admin/payments/needs-review-count
     * Returns count of payments currently flagged for review (used for sidebar badge).
     */
    @GetMapping("/needs-review-count")
    public ResponseEntity<Map<String, Long>> getNeedsReviewCount() {
        long count = adminPaymentService.countNeedsReview();
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * PATCH /api/admin/payments/{id}/resolve-review
     * Resolves a flagged payment with an admin explanation note.
     */
    @PatchMapping("/{id}/resolve-review")
    public ResponseEntity<PaymentDetailResponse> resolveReview(
            @PathVariable Long id,
            @Valid @RequestBody PaymentReviewRequest request,
            Authentication authentication
    ) {
        Long adminUserId = extractAdminUserId(authentication);
        log.info("Admin action: resolve review for paymentId={} by adminUserId={}", id, adminUserId);
        PaymentDetailResponse response = adminPaymentService.resolveReview(id, request, adminUserId);
        return ResponseEntity.ok(response);
    }

    /**
     * PATCH /api/admin/payments/{id}/refund
     * Marks a payment as REFUNDED and updates the associated order.
     */
    @PatchMapping("/{id}/refund")
    public ResponseEntity<PaymentDetailResponse> recordRefund(
            @PathVariable Long id,
            @Valid @RequestBody PaymentReviewRequest request,
            Authentication authentication
    ) {
        Long adminUserId = extractAdminUserId(authentication);
        log.info("Admin action: record refund for paymentId={} by adminUserId={}", id, adminUserId);
        PaymentDetailResponse response = adminPaymentService.recordRefund(id, request, adminUserId);
        return ResponseEntity.ok(response);
    }

    private Long extractAdminUserId(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        String email = authentication.getName();
        Long userId = customerLookupService.findUserIdByEmail(email);
        return userId != null ? userId : 0L;
    }
}
