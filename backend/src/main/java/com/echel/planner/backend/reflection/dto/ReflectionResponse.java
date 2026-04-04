package com.echel.planner.backend.reflection.dto;

import com.echel.planner.backend.reflection.DailyReflection;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReflectionResponse(
        UUID id,
        UUID userId,
        LocalDate reflectionDate,
        short energyRating,
        short moodRating,
        String reflectionNotes,
        boolean isFinalized,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReflectionResponse from(DailyReflection r) {
        return new ReflectionResponse(
                r.getId(), r.getUser().getId(), r.getReflectionDate(),
                r.getEnergyRating(), r.getMoodRating(), r.getReflectionNotes(),
                r.isFinalized(), r.getCreatedAt(), r.getUpdatedAt());
    }
}
