package com.echel.planner.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record AdminTaskRequest(
        @NotNull UUID userId,
        @NotNull UUID projectId,
        @NotBlank String title,
        String description,
        UUID parentTaskId,
        String status,
        Short priority,
        Short pointsEstimate,
        Integer actualMinutes,
        String energyLevel,
        LocalDate dueDate,
        Integer sortOrder,
        UUID blockedByTaskId
) {}
