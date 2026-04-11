package com.echel.planner.backend.schedule.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record SavePlanRequest(
        @NotNull LocalDate blockDate,
        @NotNull List<@Valid BlockEntry> blocks,
        Integer startHour,
        Integer endHour
) {
    public record BlockEntry(
            @NotNull UUID taskId,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime
    ) {}
}
