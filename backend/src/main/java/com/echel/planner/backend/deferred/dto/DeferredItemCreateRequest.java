package com.echel.planner.backend.deferred.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeferredItemCreateRequest(
        @NotBlank @Size(max = 2000) String rawText
) {}
