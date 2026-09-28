package com.allearning.backend.user.controller;

import com.allearning.backend.user.dto.AddressRequest;
import com.allearning.backend.user.dto.AddressResponse;
import com.allearning.backend.user.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for delivery address operations.
 * All endpoints require authentication (enforced by Spring Security).
 * Ownership is enforced in {@link UserProfileService} — any address that does
 * not belong to the calling user returns 404, not 403, to prevent ID probing (SEC-06).
 */
@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final UserProfileService userProfileService;

    public AddressController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    /**
     * GET /api/addresses
     * Lists all addresses belonging to the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<AddressResponse>> list(Authentication authentication) {
        return ResponseEntity.ok(userProfileService.listAddresses(authentication.getName()));
    }

    /**
     * POST /api/addresses
     * Creates a new address. First address auto-becomes default.
     */
    @PostMapping
    public ResponseEntity<AddressResponse> create(
            Authentication authentication,
            @Valid @RequestBody AddressRequest request
    ) {
        AddressResponse created = userProfileService.createAddress(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * GET /api/addresses/{id}
     * Returns a single address (ownership checked — returns 404 on mismatch).
     */
    @GetMapping("/{id}")
    public ResponseEntity<AddressResponse> get(
            Authentication authentication,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(userProfileService.getAddress(authentication.getName(), id));
    }

    /**
     * PATCH /api/addresses/{id}
     * Updates a single address (ownership checked).
     */
    @PatchMapping("/{id}")
    public ResponseEntity<AddressResponse> update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request
    ) {
        return ResponseEntity.ok(userProfileService.updateAddress(authentication.getName(), id, request));
    }

    /**
     * DELETE /api/addresses/{id}
     * Deletes an address. If it was default and others remain, promotes the most recent one.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable Long id
    ) {
        userProfileService.deleteAddress(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
