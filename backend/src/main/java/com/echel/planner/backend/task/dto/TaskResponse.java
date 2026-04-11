package com.echel.planner.backend.task.dto;

import com.echel.planner.backend.task.DeadlineGroup;
import com.echel.planner.backend.task.EnergyLevel;
import com.echel.planner.backend.task.SchedulingScope;
import com.echel.planner.backend.task.Task;
import com.echel.planner.backend.task.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        UUID projectId,
        String projectName,
        String projectColor,
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
        LocalDate visibleFrom,
        SchedulingScope schedulingScope,
        int deferralCount,
        Instant archivedAt,
        Instant completedAt,
        Instant cancelledAt,
        Instant createdAt,
        Instant updatedAt,
        List<TaskResponse> children
) {
    public static TaskResponse from(Task task, DeadlineGroup deadlineGroup, List<TaskResponse> children) {
        return new TaskResponse(
                task.getId(),
                task.getProject().getId(),
                task.getProject().getName(),
                task.getProject().getColor(),
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
                task.getVisibleFrom(),
                task.getSchedulingScope(),
                task.getDeferralCount(),
                task.getArchivedAt(),
                task.getCompletedAt(),
                task.getCancelledAt(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                children
        );
    }
}
