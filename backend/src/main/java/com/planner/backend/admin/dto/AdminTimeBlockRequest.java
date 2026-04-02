package com.planner.backend.admin.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AdminTimeBlockRequest(
        @NotNull UUID userId,
        @NotNull LocalDate blockDate,
        UUID taskId,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        Integer sortOrder,
        Boolean wasCompleted
) {}
