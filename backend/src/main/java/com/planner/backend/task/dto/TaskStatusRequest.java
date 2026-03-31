package com.planner.backend.task.dto;

import com.planner.backend.task.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record TaskStatusRequest(
        @NotNull TaskStatus status
) {}
