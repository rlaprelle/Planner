package com.echel.planner.backend.auth.dto;

import com.echel.planner.backend.auth.AppUser;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record PreferencesResponse(
        String displayName,
        String timezone,
        LocalTime defaultStartTime,
        LocalTime defaultEndTime,
        int defaultSessionMinutes,
        DayOfWeek weekStartDay,
        DayOfWeek ceremonyDay
) {
    public static PreferencesResponse from(AppUser user) {
        return new PreferencesResponse(
                user.getDisplayName(),
                user.getTimezone(),
                user.getDefaultStartTime(),
                user.getDefaultEndTime(),
                user.getDefaultSessionMinutes(),
                user.getWeekStartDay(),
                user.getCeremonyDay()
        );
    }
}
