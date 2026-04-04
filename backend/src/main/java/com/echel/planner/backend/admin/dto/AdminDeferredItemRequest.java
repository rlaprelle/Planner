package com.echel.planner.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record AdminDeferredItemRequest(
        @NotNull UUID userId,
        @NotBlank String rawText,
        Boolean isProcessed,
        UUID resolvedTaskId,
        UUID resolvedProjectId,
        LocalDate deferredUntilDate,
        Integer deferralCount
) {}
