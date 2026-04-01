package com.planner.backend.schedule.dto;

import jakarta.validation.constraints.NotNull;

public record ExtendRequest(@NotNull Integer durationMinutes) {}
