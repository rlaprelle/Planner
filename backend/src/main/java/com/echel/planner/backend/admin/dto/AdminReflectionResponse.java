package com.planner.backend.admin.dto;

import com.planner.backend.reflection.DailyReflection;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AdminReflectionResponse(
        UUID id,
        UUID userId,
        String userEmail,
        LocalDate reflectionDate,
        short energyRating,
        short moodRating,
        String reflectionNotes,
        boolean isFinalized,
        Instant createdAt,
        Instant updatedAt
) {
    public static AdminReflectionResponse from(DailyReflection r) {
        return new AdminReflectionResponse(
                r.getId(),
                r.getUser().getId(),
                r.getUser().getEmail(),
                r.getReflectionDate(),
                r.getEnergyRating(),
                r.getMoodRating(),
                r.getReflectionNotes(),
                r.isFinalized(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
