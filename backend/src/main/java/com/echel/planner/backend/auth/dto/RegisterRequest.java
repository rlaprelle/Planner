package com.echel.planner.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String displayName,
        String timezone
) {
    public RegisterRequest {
        if (timezone == null || timezone.isBlank()) {
            timezone = "UTC";
        }
    }
}
