package com.echel.planner.backend.auth.dto;

import com.echel.planner.backend.auth.AppUser;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record PreferencesResponse(
        String displayName,
        String timezone,
        @JsonFormat(pattern = "HH:mm:ss") LocalTime defaultStartTime,
        @JsonFormat(pattern = "HH:mm:ss") LocalTime defaultEndTime,
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
