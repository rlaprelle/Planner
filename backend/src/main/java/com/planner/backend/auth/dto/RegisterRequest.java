package com.planner.backend.auth.dto;

public record RegisterRequest(
        String email,
        String password,
        String displayName,
        String timezone
) {
    public RegisterRequest {
        if (timezone == null || timezone.isBlank()) {
            timezone = "UTC";
        }
    }
}
