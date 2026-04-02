package com.planner.backend.admin.dto;

import com.planner.backend.schedule.TimeBlock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AdminTimeBlockResponse(
        UUID id,
        UUID userId,
        String userEmail,
        LocalDate blockDate,
        UUID taskId,
        String taskTitle,
        LocalTime startTime,
        LocalTime endTime,
        int sortOrder,
        Instant actualStart,
        Instant actualEnd,
        boolean wasCompleted
) {
    public static AdminTimeBlockResponse from(TimeBlock tb) {
        return new AdminTimeBlockResponse(
                tb.getId(),
                tb.getUser().getId(),
                tb.getUser().getEmail(),
                tb.getBlockDate(),
                tb.getTask() != null ? tb.getTask().getId() : null,
                tb.getTask() != null ? tb.getTask().getTitle() : null,
                tb.getStartTime(),
                tb.getEndTime(),
                tb.getSortOrder(),
                tb.getActualStart(),
                tb.getActualEnd(),
                tb.isWasCompleted()
        );
    }
}
