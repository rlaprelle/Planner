package com.echel.planner.backend.task;

import java.time.LocalDate;

public enum DeadlineGroup {
    TODAY,
    THIS_WEEK,
    NO_DEADLINE;

    /** Classifies a due date relative to today and a rolling end-of-week boundary. */
    public static DeadlineGroup fromDueDate(LocalDate dueDate, LocalDate today, LocalDate endOfWeek) {
        if (dueDate == null || dueDate.isAfter(endOfWeek)) return NO_DEADLINE;
        if (!dueDate.isAfter(today)) return TODAY;
        return THIS_WEEK;
    }
}
