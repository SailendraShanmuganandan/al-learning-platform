package com.allearning.backend.commerce.controller;

import com.allearning.backend.auth.User;
import com.allearning.backend.auth.UserRole;
import com.allearning.backend.commerce.dto.PaymentCreateRequest;
import com.allearning.backend.commerce.dto.PaymentCreateResponse;
import com.allearning.backend.commerce.dto.PaymentDetailResponse;
import com.allearning.backend.commerce.exception.PaymentException;
import com.allearning.backend.commerce.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for payments and PayHere webhook notifications.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * POST /api/payments/create
     * Prepares payment redirect parameters for the student's order.
     */
    @PostMapping("/create")
    public ResponseEntity<PaymentCreateResponse> createPayment(
            @AuthenticationPrincipal User currentUser,
            @RequestBody PaymentCreateRequest request
    ) {
        if (currentUser == null) {
            throw new PaymentException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required.");
        }
        if (currentUser.getRole() != UserRole.STUDENT) {
            throw new PaymentException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only students can initiate payments.");
        }

        Long orderId = request != null ? request.getOrderId() : null;
        PaymentCreateResponse response = paymentService.createPayment(currentUser.getUserId(), orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/payments/webhook
     * Public server-to-server callback endpoint for PayHere payment notifications.
     */
    @PostMapping(value = "/webhook", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> handleWebhook(@RequestParam Map<String, String> params) {
        int statusCode = paymentService.processWebhookNotification(params);
        return ResponseEntity.status(statusCode).build();
    }

    /**
     * GET /api/payments/{id}
     * Retrieves payment details (accessible to owning student or ADMIN).
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentDetailResponse> getPayment(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        if (currentUser == null) {
            throw new PaymentException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required.");
        }

        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;
        PaymentDetailResponse response = paymentService.getPayment(id, currentUser.getUserId(), isAdmin);
        return ResponseEntity.ok(response);
    }
}
