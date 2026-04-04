package com.echel.planner.backend.deferred.dto;

import com.echel.planner.backend.task.EnergyLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Request to convert a deferred item into a calendar event.
 */
public record ConvertToEventRequest(
        @NotNull UUID projectId,
        @NotBlank @Size(max = 255) String title,
        String description,
        EnergyLevel energyLevel,
        @NotNull LocalDate blockDate,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime
) {}
