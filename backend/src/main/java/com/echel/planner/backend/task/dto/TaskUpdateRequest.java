package com.echel.planner.backend.task.dto;

import com.echel.planner.backend.task.EnergyLevel;
import com.echel.planner.backend.task.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record TaskUpdateRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        TaskStatus status,
        Short priority,
        Short pointsEstimate,
        Integer actualMinutes,
        EnergyLevel energyLevel,
        LocalDate dueDate,
        Integer sortOrder,
        UUID projectId
) {}
