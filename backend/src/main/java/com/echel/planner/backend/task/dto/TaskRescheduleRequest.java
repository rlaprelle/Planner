package com.echel.planner.backend.task.dto;

import com.echel.planner.backend.task.SchedulingScope;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record TaskRescheduleRequest(
        @NotNull LocalDate visibleFrom,
        SchedulingScope schedulingScope
) {}
