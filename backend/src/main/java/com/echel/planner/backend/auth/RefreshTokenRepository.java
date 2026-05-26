package com.echel.planner.backend.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByUserId(UUID userId);

    /**
     * Revoke every still-active token for a user in a single statement. Used
     * when reuse is detected — we don't try to walk the parent_id chain
     * surgically; revoking all of the user's outstanding tokens is both simpler
     * and the safer response to suspected theft.
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = :now " +
           "WHERE rt.userId = :userId AND rt.revokedAt IS NULL")
    int revokeAllActiveForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}
