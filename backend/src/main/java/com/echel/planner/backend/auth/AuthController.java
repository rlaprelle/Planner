package com.echel.planner.backend.auth;

import com.echel.planner.backend.auth.dto.AuthResponse;
import com.echel.planner.backend.auth.dto.ConfirmEmailChangeRequest;
import com.echel.planner.backend.auth.dto.EmailChangeConfirmedResponse;
import com.echel.planner.backend.auth.dto.LoginRequest;
import com.echel.planner.backend.auth.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AccountService accountService;

    public AuthController(AuthService authService, AccountService accountService) {
        this.authService = authService;
        this.accountService = accountService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthService.AuthResult result = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, result.refreshCookie().toString())
                .body(result.authResponse());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.AuthResult result = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, result.refreshCookie().toString())
                .body(result.authResponse());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
        AuthService.AuthResult result = authService.refresh(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, result.refreshCookie().toString())
                .body(result.authResponse());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authService.logout(request).toString())
                .build();
    }

    /**
     * Completes an email change from the verification link. Public (no access
     * token) because possession of the single-use token — delivered only to the
     * new address — is the proof of control. On success every session is revoked,
     * so the client must sign back in with the new email.
     */
    @PostMapping("/email/confirm")
    public ResponseEntity<EmailChangeConfirmedResponse> confirmEmailChange(
            @Valid @RequestBody ConfirmEmailChangeRequest request) {
        String newEmail = accountService.confirmEmailChange(request.token());
        return ResponseEntity.ok(new EmailChangeConfirmedResponse(newEmail));
    }
}
