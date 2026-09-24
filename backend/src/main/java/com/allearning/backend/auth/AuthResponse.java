package com.allearning.backend.auth;

/**
 * Outbound response DTO for successful login.
 */
public record AuthResponse(
        String token,
        String tokenType,
        UserResponse user
) {
    public static AuthResponse of(String token, User user) {
        return new AuthResponse(token, "Bearer", UserResponse.from(user));
    }
}
