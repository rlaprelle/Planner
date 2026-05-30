package com.echel.planner.backend.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface EmailChangeTokenRepository extends JpaRepository<EmailChangeToken, UUID> {

    Optional<EmailChangeToken> findByTokenHash(String tokenHash);

    /**
     * Consume every still-pending request for a user in a single statement. A new
     * email-change request supersedes any older outstanding ones — only the most
     * recent link should remain clickable.
     */
    @Modifying
    @Query("UPDATE EmailChangeToken t SET t.consumedAt = :now " +
           "WHERE t.userId = :userId AND t.consumedAt IS NULL")
    int consumeAllPendingForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}
