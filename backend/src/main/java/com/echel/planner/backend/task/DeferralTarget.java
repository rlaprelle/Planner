package com.echel.planner.backend.task;

/** Where a task is being deferred to during End Day triage. */
public enum DeferralTarget {
    TOMORROW,
    NEXT_WEEK,
    NEXT_MONTH
}
