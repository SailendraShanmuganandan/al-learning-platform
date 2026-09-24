package com.allearning.backend.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

/**
 * Outbound response DTO for a user profile.
 * The {@code passwordHash} field is intentionally excluded to prevent
 * leaking credential data in API responses.
 */
public record UserResponse(
        Long userId,
        String fullName,
        String email,
        String phone,
        String role,
        String accountStatus,
        @JsonProperty("isEmailVerified")
        boolean isEmailVerified,
        OffsetDateTime createdAt
) {
    /** Convenience factory that maps from the JPA entity. */
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().name(),
                user.getAccountStatus().name(),
                user.isEmailVerified(),
                user.getCreatedAt()
        );
    }

    /** Expose snake_case property alias for API consumers expecting is_email_verified */
    @JsonProperty("is_email_verified")
    public boolean getIsEmailVerifiedSnake() {
        return isEmailVerified;
    }
}
