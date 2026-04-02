package com.planner.backend.admin.dto;

import com.planner.backend.task.Task;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AdminTaskResponse(
        UUID id,
        UUID userId,
        String userEmail,
        UUID projectId,
        String projectName,
        String title,
        String description,
        UUID parentTaskId,
        String status,
        short priority,
        Short pointsEstimate,
        Integer actualMinutes,
        String energyLevel,
        LocalDate dueDate,
        int sortOrder,
        UUID blockedByTaskId,
        Instant archivedAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static AdminTaskResponse from(Task task) {
        return new AdminTaskResponse(
                task.getId(),
                task.getUser().getId(),
                task.getUser().getEmail(),
                task.getProject().getId(),
                task.getProject().getName(),
                task.getTitle(),
                task.getDescription(),
                task.getParentTask() != null ? task.getParentTask().getId() : null,
                task.getStatus().name(),
                task.getPriority(),
                task.getPointsEstimate(),
                task.getActualMinutes(),
                task.getEnergyLevel() != null ? task.getEnergyLevel().name() : null,
                task.getDueDate(),
                task.getSortOrder(),
                task.getBlockedByTask() != null ? task.getBlockedByTask().getId() : null,
                task.getArchivedAt(),
                task.getCompletedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
