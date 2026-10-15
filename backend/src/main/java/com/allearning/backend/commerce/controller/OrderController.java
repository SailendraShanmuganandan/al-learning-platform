package com.allearning.backend.commerce.controller;

import com.allearning.backend.auth.AuthService;
import com.allearning.backend.auth.User;
import com.allearning.backend.commerce.dto.CheckoutRequest;
import com.allearning.backend.commerce.dto.OrderResponse;
import com.allearning.backend.commerce.service.OrderService;
import com.allearning.backend.security.RequiresEmailVerified;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final AuthService authService;

    public OrderController(OrderService orderService, AuthService authService) {
        this.orderService = orderService;
        this.authService = authService;
    }

    /**
     * POST /api/orders
     * Creates an order from the user's active cart.
     * Requires verified student email (FR-AUTH-14).
     */
    @PostMapping
    @RequiresEmailVerified
    public ResponseEntity<OrderResponse> checkout(
            Authentication authentication,
            @Valid @RequestBody(required = false) CheckoutRequest request
    ) {
        Long userId = extractUserId(authentication);
        OrderResponse response = orderService.checkout(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/orders
     * Returns paginated orders for the authenticated student, newest first.
     */
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getOrders(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Long userId = extractUserId(authentication);
        PageRequest pageRequest = PageRequest.of(page, Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(orderService.getUserOrders(userId, pageRequest));
    }

    /**
     * GET /api/orders/{id}
     * Returns order details. Ownership enforced (404 on mismatch).
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(
            Authentication authentication,
            @PathVariable Long id
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(orderService.getUserOrder(userId, id));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user.getUserId();
        }
        String email = authentication.getName();
        return authService.getMe(email).userId();
    }
}
