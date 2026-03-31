package com.planner.backend.task.dto;

import com.planner.backend.task.DeadlineGroup;
import com.planner.backend.task.EnergyLevel;
import com.planner.backend.task.Task;
import com.planner.backend.task.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        UUID projectId,
        UUID userId,
        String title,
        String description,
        UUID parentTaskId,
        TaskStatus status,
        short priority,
        Short pointsEstimate,
        Integer actualMinutes,
        EnergyLevel energyLevel,
        LocalDate dueDate,
        int sortOrder,
        UUID blockedByTaskId,
        DeadlineGroup deadlineGroup,
        Instant archivedAt,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt,
        List<TaskResponse> children
) {
    public static TaskResponse from(Task task, DeadlineGroup deadlineGroup, List<TaskResponse> children) {
        return new TaskResponse(
                task.getId(),
                task.getProject().getId(),
                task.getUser().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getParentTask() != null ? task.getParentTask().getId() : null,
                task.getStatus(),
                task.getPriority(),
                task.getPointsEstimate(),
                task.getActualMinutes(),
                task.getEnergyLevel(),
                task.getDueDate(),
                task.getSortOrder(),
                task.getBlockedByTask() != null ? task.getBlockedByTask().getId() : null,
                deadlineGroup,
                task.getArchivedAt(),
                task.getCompletedAt(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                children
        );
    }
}
