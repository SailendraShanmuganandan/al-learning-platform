package com.allearning.backend.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * REST controller for authentication endpoints.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/auth/register
     * <p>
     * Public endpoint. Validates the request body, registers a new user account,
     * issues an email verification token, and returns the created user's safe fields with HTTP 201 Created.
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/login
     * <p>
     * Public endpoint. Validates credentials and returns a signed JWT token + user details.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/auth/me
     * <p>
     * Protected endpoint. Requires Authorization: Bearer &lt;token&gt;.
     * Returns the currently authenticated user's details.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal Object principal) {
        String email = extractEmail(principal);
        UserResponse response = authService.getMe(email);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/auth/verify-email?token=...
     * <p>
     * Public endpoint. Validates the token and marks the user's email as verified.
     * Idempotent-safe: returns 400 Bad Request if token is expired, invalid, or already used.
     */
    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam("token") String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message", "Email successfully verified. You may now use all platform features."));
    }

    /**
     * POST /api/auth/resend-verification
     * <p>
     * Authenticated endpoint. Invalidates any existing unexpired token, generates a new one,
     * and sends a fresh verification email. Returns a generic acknowledgment to avoid leaking state.
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(@AuthenticationPrincipal Object principal) {
        String email = extractEmail(principal);
        authService.resendVerification(email);
        return ResponseEntity.ok(Map.of("message", "If your account requires verification, a new link has been sent."));
    }

    /**
     * POST /api/auth/forgot-password
     * <p>
     * Public endpoint. Always returns the same generic success message regardless of whether
     * the account exists, to prevent user-enumeration attacks (SEC requirement).
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return ResponseEntity.ok(Map.of(
                "message", "If an account with that email exists, a password reset link has been sent."
        ));
    }

    /**
     * POST /api/auth/reset-password
     * <p>
     * Public endpoint. Validates the token and sets the new password.
     * Returns 400 if the token is invalid, expired, or already used.
     * Does NOT auto-login the user — they log in normally afterward.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successfully. You may now log in with your new password."));
    }

    private String extractEmail(Object principal) {
        if (principal instanceof User user) {
            return user.getEmail();
        } else if (principal instanceof UserDetails ud) {
            return ud.getUsername();
        } else if (principal instanceof String str) {
            return str;
        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }
}
