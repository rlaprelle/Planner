package com.echel.planner.backend.project.dto;

import com.echel.planner.backend.project.Project;

import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        String name,
        String description,
        String color,
        String icon,
        boolean isActive,
        int sortOrder,
        Instant archivedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getColor(),
                project.getIcon(),
                project.isActive(),
                project.getSortOrder(),
                project.getArchivedAt(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
