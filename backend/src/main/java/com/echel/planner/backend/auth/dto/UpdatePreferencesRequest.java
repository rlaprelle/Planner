package com.echel.planner.backend.auth.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Partial-update request for user preferences. Any null field is left unchanged.
 */
public record UpdatePreferencesRequest(
        String displayName,
        String timezone,
        LocalTime defaultStartTime,
        LocalTime defaultEndTime,
        Integer defaultSessionMinutes,
        DayOfWeek weekStartDay,
        DayOfWeek ceremonyDay
) {}
