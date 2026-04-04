package com.echel.planner.backend.event.dto;

import com.echel.planner.backend.task.EnergyLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record EventCreateRequest(
        @NotNull UUID projectId,
        @NotBlank @Size(max = 255) String title,
        String description,
        EnergyLevel energyLevel,
        @NotNull LocalDate blockDate,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime
) {}
