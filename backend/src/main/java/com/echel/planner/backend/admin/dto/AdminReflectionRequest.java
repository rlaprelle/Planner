package com.echel.planner.backend.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record AdminReflectionRequest(
        @NotNull UUID userId,
        @NotNull LocalDate reflectionDate,
        @NotNull @Min(1) @Max(5) Short energyRating,
        @NotNull @Min(1) @Max(5) Short moodRating,
        String reflectionNotes,
        Boolean isFinalized
) {}
