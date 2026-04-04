package com.echel.planner.backend.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectUpdateRequest(
        @NotBlank @Size(max = 255) String name,
        String description,
        @Size(max = 7) String color,
        @Size(max = 50) String icon,
        Integer sortOrder
) {}
