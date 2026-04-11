package com.echel.planner.backend.auth.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record UpdatePreferencesRequest(
        String displayName,
        String timezone,
        LocalTime defaultStartTime,
        LocalTime defaultEndTime,
        Integer defaultSessionMinutes,
        DayOfWeek weekStartDay,
        DayOfWeek ceremonyDay
) {}
