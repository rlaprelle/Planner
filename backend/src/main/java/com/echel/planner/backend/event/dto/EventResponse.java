package com.echel.planner.backend.event.dto;

import com.echel.planner.backend.event.Event;
import com.echel.planner.backend.task.EnergyLevel;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record EventResponse(
        UUID id,
        UUID projectId,
        String projectName,
        String projectColor,
        UUID userId,
        String title,
        String description,
        EnergyLevel energyLevel,
        LocalDate blockDate,
        LocalTime startTime,
        LocalTime endTime,
        Instant archivedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static EventResponse from(Event event) {
        return new EventResponse(
                event.getId(),
                event.getProject().getId(),
                event.getProject().getName(),
                event.getProject().getColor(),
                event.getUser().getId(),
                event.getTitle(),
                event.getDescription(),
                event.getEnergyLevel(),
                event.getBlockDate(),
                event.getStartTime(),
                event.getEndTime(),
                event.getArchivedAt(),
                event.getCreatedAt(),
                event.getUpdatedAt()
        );
    }
}
