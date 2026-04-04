package com.echel.planner.backend.task.dto;

import com.echel.planner.backend.task.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusRequest(
        @NotNull TaskStatus status
) {}
