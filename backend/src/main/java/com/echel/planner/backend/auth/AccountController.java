package com.echel.planner.backend.auth;

import com.echel.planner.backend.auth.dto.AuthResponse;
import com.echel.planner.backend.auth.dto.ChangeEmailRequest;
import com.echel.planner.backend.auth.dto.ChangePasswordRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authenticated account-management endpoints for the current user: password
 * change and email-change initiation. The confirmation step lives on
 * {@link AuthController} because it must work without a valid access token.
 */
@RestController
@RequestMapping("/api/v1/user")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/password")
    public ResponseEntity<AuthResponse> changePassword(
            @AuthenticationPrincipal AppUser user,
            @Valid @RequestBody ChangePasswordRequest request) {
        // Rotates the session: a fresh refresh cookie keeps the calling client
        // signed in even though every prior session was just revoked.
        AuthService.AuthResult result = accountService.changePassword(user, request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, result.refreshCookie().toString())
                .body(result.authResponse());
    }

    @PostMapping("/email")
    public ResponseEntity<Void> requestEmailChange(
            @AuthenticationPrincipal AppUser user,
            @Valid @RequestBody ChangeEmailRequest request) {
        accountService.requestEmailChange(user, request);
        // 202: the change is pending verification, not yet applied.
        return ResponseEntity.accepted().build();
    }
}
