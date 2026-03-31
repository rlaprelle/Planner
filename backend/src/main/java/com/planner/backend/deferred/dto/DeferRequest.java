package com.planner.backend.deferred.dto;

import jakarta.validation.constraints.NotNull;

public record DeferRequest(@NotNull DeferDuration deferFor) {
    public enum DeferDuration { ONE_DAY, ONE_WEEK, ONE_MONTH }
}
