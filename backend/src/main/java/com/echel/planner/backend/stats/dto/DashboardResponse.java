package com.planner.backend.stats.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DashboardResponse(
        int todayBlockCount,
        int todayCompletedCount,
        int streakDays,
        List<DeadlineSummary> upcomingDeadlines,
        int deferredItemCount
) {
    public record DeadlineSummary(
            UUID taskId,
            String taskTitle,
            String projectName,
            String projectColor,
            LocalDate dueDate,
            String deadlineGroup
    ) {}
}
