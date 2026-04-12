package com.echel.planner.backend.stats.dto;

import com.echel.planner.backend.stats.CelebrationReason;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DashboardResponse(
        int todayBlockCount,
        int todayCompletedCount,
        int streakDays,
        List<DeadlineSummary> upcomingDeadlines,
        int deferredItemCount,
        List<CelebrationTask> celebrationTasks
) {
    public record DeadlineSummary(
            UUID taskId,
            String taskTitle,
            String projectName,
            String projectColor,
            LocalDate dueDate,
            String deadlineGroup
    ) {}

    public record CelebrationTask(
            UUID taskId,
            String taskTitle,
            String projectName,
            CelebrationReason celebrationReason,
            String reason
    ) {}
}
