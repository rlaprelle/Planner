package com.echel.planner.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request to begin changing the authenticated user's login email. The current
 * password re-authenticates the request; a verification link is then sent to
 * {@code newEmail} and the address only switches once that link is followed.
 */
public record ChangeEmailRequest(
        @NotBlank @Email String newEmail,
        @NotBlank String currentPassword
) {
}
