package com.echel.planner.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AdminProjectRequest(
        @NotNull UUID userId,
        @NotBlank @Size(max = 255) String name,
        String description,
        @Size(max = 7) String color,
        @Size(max = 50) String icon,
        Boolean isActive,
        Integer sortOrder
) {}
