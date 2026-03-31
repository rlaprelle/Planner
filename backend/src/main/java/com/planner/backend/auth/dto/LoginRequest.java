package com.planner.backend.auth.dto;

public record LoginRequest(
        String email,
        String password
) {}
