package com.planner.backend.deferred.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record ConvertToTaskRequest(
        @NotNull UUID projectId,
        @NotBlank @Size(max = 255) String title,
        String description,
        LocalDate dueDate,
        Short priority,
        Short pointsEstimate
) {}
