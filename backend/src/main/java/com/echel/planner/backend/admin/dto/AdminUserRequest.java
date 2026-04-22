package com.echel.planner.backend.admin.dto;

import com.echel.planner.backend.auth.AppUser;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUserRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @Size(min = 6) String password,
        @NotBlank @Size(max = 255) String displayName,
        @Size(max = 100) String timezone,
        @NotNull AppUser.Role role
) {}
