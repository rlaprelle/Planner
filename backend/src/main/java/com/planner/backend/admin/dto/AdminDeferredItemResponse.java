package com.planner.backend.admin.dto;

import com.planner.backend.deferred.DeferredItem;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AdminDeferredItemResponse(
        UUID id,
        UUID userId,
        String userEmail,
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
    public static AdminDeferredItemResponse from(DeferredItem item) {
        return new AdminDeferredItemResponse(
                item.getId(),
                item.getUser().getId(),
                item.getUser().getEmail(),
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
