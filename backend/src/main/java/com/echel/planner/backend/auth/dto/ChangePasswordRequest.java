package com.echel.planner.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to change the authenticated user's password. The current password is
 * required as a re-authentication step so a hijacked, still-open session can't
 * silently lock the real owner out.
 */
public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8) String newPassword
) {
}
