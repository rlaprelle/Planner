package com.planner.backend.admin.dto;

import com.planner.backend.auth.AppUser;
import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String email,
        String displayName,
        String timezone,
        Instant createdAt,
        Instant updatedAt
) {
    public static AdminUserResponse from(AppUser user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getTimezone(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
