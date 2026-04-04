package com.echel.planner.backend.admin.dto;

public record DependentCountResponse(
        long projects,
        long tasks,
        long deferredItems,
        long reflections,
        long timeBlocks
) {}
