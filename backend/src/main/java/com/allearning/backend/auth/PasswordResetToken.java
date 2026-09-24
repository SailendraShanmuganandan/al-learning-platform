package com.allearning.backend.auth;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * Stores one-time password-reset tokens.
 * <p>
 * A token is considered <em>usable</em> only when all of the following hold:
 * <ul>
 *   <li>it exists in the database</li>
 *   <li>{@code usedAt} is {@code null} (not yet consumed)</li>
 *   <li>{@code expiresAt} is in the future</li>
 * </ul>
 */
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long tokenId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected PasswordResetToken() {}

    public PasswordResetToken(User user, String token, OffsetDateTime expiresAt) {
        this.user = user;
        this.token = token;
        this.expiresAt = expiresAt;
        this.createdAt = OffsetDateTime.now();
    }

    // --- Queries ---

    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public void markUsed() {
        this.usedAt = OffsetDateTime.now();
    }

    // --- Accessors ---

    public Long getTokenId() { return tokenId; }
    public User getUser()    { return user; }
    public String getToken() { return token; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public OffsetDateTime getUsedAt()    { return usedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
