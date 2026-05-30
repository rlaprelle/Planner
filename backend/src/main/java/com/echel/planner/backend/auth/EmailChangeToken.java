package com.echel.planner.backend.auth;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * A pending request to change a user's login email to {@link #newEmail}.
 *
 * <p>The raw token only ever travels in the verification link sent to the new
 * address; what we persist is its SHA-256 hash. A token is single-use: once
 * redeemed, {@link #consumedAt} is set and it can never switch an email again.
 *
 * <p>See {@code V16__create_email_change_token.sql} for the rationale behind
 * each column.
 */
@Entity
@Table(name = "email_change_token")
public class EmailChangeToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "new_email", nullable = false)
    private String newEmail;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    protected EmailChangeToken() {}

    public EmailChangeToken(UUID userId, String newEmail, String tokenHash, Instant expiresAt) {
        this.userId = userId;
        this.newEmail = newEmail;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getNewEmail() { return newEmail; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getConsumedAt() { return consumedAt; }
    public Instant getCreatedAt() { return createdAt; }

    public boolean isConsumed() {
        return consumedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public void consume(Instant at) {
        if (consumedAt == null) {
            this.consumedAt = at;
        }
    }
}
