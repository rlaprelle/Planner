package com.echel.planner.backend.auth;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A server-side record of an outstanding (or formerly outstanding) refresh token.
 *
 * <p>The raw token only ever lives in the user's cookie; what we persist is the
 * SHA-256 hash. {@link #parentId} threads tokens into rotation chains so that
 * detecting reuse of a revoked token lets us revoke every descendant in one go.
 *
 * <p>See {@code V13__create_refresh_token.sql} for the rationale behind each
 * column.
 */
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(nullable = false, unique = true)
    private UUID jti;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected RefreshToken() {}

    public RefreshToken(UUID userId, String tokenHash, UUID jti, UUID parentId, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.jti = jti;
        this.parentId = parentId;
        this.expiresAt = expiresAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public UUID getJti() { return jti; }
    public UUID getParentId() { return parentId; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public Instant getCreatedAt() { return createdAt; }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public void revoke(Instant at) {
        if (revokedAt == null) {
            this.revokedAt = at;
        }
    }
}
