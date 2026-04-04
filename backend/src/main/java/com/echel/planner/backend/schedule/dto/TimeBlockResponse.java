package com.planner.backend.schedule.dto;

import com.planner.backend.schedule.TimeBlock;
import com.planner.backend.task.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record TimeBlockResponse(
        UUID id,
        LocalDate blockDate,
        LocalTime startTime,
        LocalTime endTime,
        int sortOrder,
        Instant actualStart,
        Instant actualEnd,
        boolean wasCompleted,
        TaskSummary task
) {
    public record TaskSummary(
            UUID id,
            String title,
            UUID projectId,
            String projectName,
            String projectColor,
            TaskStatus status,
            Short pointsEstimate
    ) {}

    public static TimeBlockResponse from(TimeBlock block) {
        TaskSummary taskSummary = null;
        if (block.getTask() != null) {
            var t = block.getTask();
            taskSummary = new TaskSummary(
                    t.getId(),
                    t.getTitle(),
                    t.getProject().getId(),
                    t.getProject().getName(),
                    t.getProject().getColor(),
                    t.getStatus(),
                    t.getPointsEstimate()
            );
        }
        return new TimeBlockResponse(
                block.getId(),
                block.getBlockDate(),
                block.getStartTime(),
                block.getEndTime(),
                block.getSortOrder(),
                block.getActualStart(),
                block.getActualEnd(),
                block.isWasCompleted(),
                taskSummary
        );
    }
}
