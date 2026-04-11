package com.echel.planner.backend.task.dto;

import com.echel.planner.backend.task.DeferralTarget;
import jakarta.validation.constraints.NotNull;

public record TaskDeferRequest(
        @NotNull DeferralTarget target
) {}
