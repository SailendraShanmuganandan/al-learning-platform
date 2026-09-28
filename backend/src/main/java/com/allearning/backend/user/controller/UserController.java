package com.allearning.backend.user.controller;

import com.allearning.backend.auth.UserResponse;
import com.allearning.backend.user.dto.UpdateProfileRequest;
import com.allearning.backend.user.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user profile operations.
 * All endpoints require authentication (enforced by {@link com.allearning.backend.security.SecurityConfig}).
 * Email-verification guard is NOT applied here per the SRS (profile management is always accessible).
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserProfileService userProfileService;

    public UserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    /**
     * GET /api/users/me
     * Returns the authenticated user's profile. Never exposes password_hash.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(Authentication authentication) {
        return ResponseEntity.ok(userProfileService.getProfile(authentication.getName()));
    }

    /**
     * PATCH /api/users/me
     * Updates full_name and phone only. Ignores any email/role/account_status in the body.
     */
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserResponse updated = userProfileService.updateProfile(
                authentication.getName(),
                request.fullName(),
                request.phone()
        );
        return ResponseEntity.ok(updated);
    }
}
