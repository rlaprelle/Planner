package com.echel.planner.backend.deferred.dto;

import com.echel.planner.backend.deferred.DeferredItem;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DeferredItemResponse(
        UUID id,
        UUID userId,
        String rawText,
        boolean isProcessed,
        Instant capturedAt,
        Instant processedAt,
        UUID resolvedTaskId,
        UUID resolvedProjectId,
        LocalDate deferredUntilDate,
        int deferralCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static DeferredItemResponse from(DeferredItem item) {
        return new DeferredItemResponse(
                item.getId(),
                item.getUser().getId(),
                item.getRawText(),
                item.isProcessed(),
                item.getCapturedAt(),
                item.getProcessedAt(),
                item.getResolvedTask() != null ? item.getResolvedTask().getId() : null,
                item.getResolvedProject() != null ? item.getResolvedProject().getId() : null,
                item.getDeferredUntilDate(),
                item.getDeferralCount(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
