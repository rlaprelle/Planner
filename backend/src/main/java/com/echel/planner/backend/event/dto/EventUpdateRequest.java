package com.echel.planner.backend.event.dto;

import com.echel.planner.backend.task.EnergyLevel;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Partial update request for an event. Only non-null fields are applied.
 */
public record EventUpdateRequest(
        UUID projectId,
        @Size(max = 255) String title,
        String description,
        EnergyLevel energyLevel,
        LocalDate blockDate,
        LocalTime startTime,
        LocalTime endTime
) {}
