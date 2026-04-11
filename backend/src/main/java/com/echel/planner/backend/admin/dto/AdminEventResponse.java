package com.echel.planner.backend.admin.dto;

import com.echel.planner.backend.event.Event;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AdminEventResponse(
        UUID id,
        UUID userId,
        String userEmail,
        UUID projectId,
        String projectName,
        String title,
        String description,
        String energyLevel,
        LocalDate blockDate,
        LocalTime startTime,
        LocalTime endTime,
        Instant archivedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static AdminEventResponse from(Event event) {
        return new AdminEventResponse(
                event.getId(),
                event.getUser().getId(),
                event.getUser().getEmail(),
                event.getProject().getId(),
                event.getProject().getName(),
                event.getTitle(),
                event.getDescription(),
                event.getEnergyLevel() != null ? event.getEnergyLevel().name() : null,
                event.getBlockDate(),
                event.getStartTime(),
                event.getEndTime(),
                event.getArchivedAt(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
