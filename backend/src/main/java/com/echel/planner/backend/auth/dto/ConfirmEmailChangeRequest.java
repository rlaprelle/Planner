package com.echel.planner.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Redeems the single-use token from an email-change verification link. No
 * access token is required — possession of the token (delivered only to the new
 * address) is the proof of control.
 */
public record ConfirmEmailChangeRequest(
        @NotBlank String token
) {
}
