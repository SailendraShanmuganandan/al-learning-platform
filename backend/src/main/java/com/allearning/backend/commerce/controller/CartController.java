package com.allearning.backend.commerce.controller;

import com.allearning.backend.auth.AuthService;
import com.allearning.backend.auth.User;
import com.allearning.backend.commerce.dto.CartItemAddRequest;
import com.allearning.backend.commerce.dto.CartItemUpdateRequest;
import com.allearning.backend.commerce.dto.CartResponse;
import com.allearning.backend.commerce.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;
    private final AuthService authService;

    public CartController(CartService cartService, AuthService authService) {
        this.cartService = cartService;
        this.authService = authService;
    }

    /**
     * GET /api/cart
     * Returns the current active cart or an empty cart representation.
     */
    @GetMapping
    public ResponseEntity<CartResponse> getCart(Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    /**
     * POST /api/cart/items
     * Adds an item to the active cart (lazily created if missing).
     */
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            Authentication authentication,
            @Valid @RequestBody CartItemAddRequest request
    ) {
        Long userId = extractUserId(authentication);
        CartResponse response = cartService.addItem(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PATCH /api/cart/items/{id}
     * Updates the quantity of a cart item.
     */
    @PatchMapping("/items/{id}")
    public ResponseEntity<CartResponse> updateItem(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody CartItemUpdateRequest request
    ) {
        Long userId = extractUserId(authentication);
        return ResponseEntity.ok(cartService.updateItemQuantity(userId, id, request));
    }

    /**
     * DELETE /api/cart/items/{id}
     * Removes an item from the active cart.
     */
    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> removeItem(
            Authentication authentication,
            @PathVariable Long id
    ) {
        Long userId = extractUserId(authentication);
        cartService.removeItem(userId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/cart/items
     * Clears all items in the active cart.
     */
    @DeleteMapping("/items")
    public ResponseEntity<Void> clearCart(Authentication authentication) {
        Long userId = extractUserId(authentication);
        cartService.clearCart(userId);
        return ResponseEntity.noContent().build();
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
