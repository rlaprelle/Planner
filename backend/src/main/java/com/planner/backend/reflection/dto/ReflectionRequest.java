package com.planner.backend.reflection.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReflectionRequest(
        @NotNull @Min(1) @Max(5) Short energyRating,
        @NotNull @Min(1) @Max(5) Short moodRating,
        String reflectionNotes,
        boolean isFinalized
) {}
