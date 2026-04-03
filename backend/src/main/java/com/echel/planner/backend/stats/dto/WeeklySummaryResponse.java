package com.echel.planner.backend.stats.dto;

/**
 * Rolling 7-day stats summary for the dashboard banner.
 */
public record WeeklySummaryResponse(
        int tasksCompleted,
        int totalPoints,
        int totalFocusMinutes,
        int streakDays,
        String energyTrend,
        String moodTrend,
        boolean hasActivity
) {}
