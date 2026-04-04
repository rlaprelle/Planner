package com.echel.planner.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AdminEventRequest(
        @NotNull UUID userId,
        @NotNull UUID projectId,
        @NotBlank @Size(max = 255) String title,
        String description,
        String energyLevel,
        @NotNull LocalDate blockDate,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime
) {}
