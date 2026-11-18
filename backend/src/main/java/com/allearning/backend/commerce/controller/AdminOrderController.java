package com.allearning.backend.commerce.controller;

import com.allearning.backend.commerce.dto.AdminOrderDetail;
import com.allearning.backend.commerce.dto.AdminOrderListItem;
import com.allearning.backend.commerce.dto.FulfillmentUpdateRequest;
import com.allearning.backend.commerce.service.AdminOrderService;
import com.allearning.backend.commerce.service.CustomerLookupService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

/**
 * Controller for admin order management operations.
 * All endpoints require ROLE_ADMIN (enforced by SecurityConfig).
 */
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private static final Logger log = LoggerFactory.getLogger(AdminOrderController.class);

    private final AdminOrderService adminOrderService;
    private final CustomerLookupService customerLookupService;

    public AdminOrderController(
            AdminOrderService adminOrderService,
            CustomerLookupService customerLookupService
    ) {
        this.adminOrderService = adminOrderService;
        this.customerLookupService = customerLookupService;
    }

    /**
     * GET /api/admin/orders
     * Paginated list of orders, sorted newest first by default.
     */
    @GetMapping
    public ResponseEntity<Page<AdminOrderListItem>> getOrders(
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String fulfillmentStatus,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean needsReview,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageRequest pageRequest = PageRequest.of(page, Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AdminOrderListItem> result = adminOrderService.listOrders(
                paymentStatus, fulfillmentStatus, q, needsReview, from, to, pageRequest
        );
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/admin/orders/{id}
     * Returns full order details.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AdminOrderDetail> getOrderDetail(@PathVariable Long id) {
        AdminOrderDetail detail = adminOrderService.getOrderDetail(id);
        return ResponseEntity.ok(detail);
    }

    /**
     * PATCH /api/admin/orders/{id}/fulfillment-status
     * Transitions order fulfillment status one step forward.
     */
    @PatchMapping("/{id}/fulfillment-status")
    public ResponseEntity<AdminOrderDetail> updateFulfillmentStatus(
            @PathVariable Long id,
            @Valid @RequestBody FulfillmentUpdateRequest request,
            Authentication authentication
    ) {
        Long adminUserId = extractAdminUserId(authentication);
        log.info("Admin action: update fulfillment status for orderId={} by adminUserId={}", id, adminUserId);
        AdminOrderDetail updated = adminOrderService.updateFulfillmentStatus(id, request, adminUserId);
        return ResponseEntity.ok(updated);
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
