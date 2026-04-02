package com.planner.backend.admin.dto;

import com.planner.backend.project.Project;
import java.time.Instant;
import java.util.UUID;

public record AdminProjectResponse(
        UUID id,
        UUID userId,
        String userEmail,
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
    public static AdminProjectResponse from(Project project) {
        return new AdminProjectResponse(
                project.getId(),
                project.getUser().getId(),
                project.getUser().getEmail(),
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
