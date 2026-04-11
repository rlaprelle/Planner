package com.echel.planner.backend.task;

/** How a task was deferred — metadata for tracking, not query filtering. */
public enum SchedulingScope {
    DAY,
    WEEK,
    MONTH
}
