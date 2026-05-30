package com.echel.planner.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.echel.planner.backend.auth.dto.ConfirmEmailChangeRequest;
import com.echel.planner.backend.auth.dto.LoginRequest;
import com.echel.planner.backend.auth.dto.RegisterRequest;
import com.echel.planner.backend.email.EmailSender;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthFilter.class, AuthService.class, AccountService.class,
         AuthExceptionHandler.class, AuthRateLimitFilter.class})
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private AppUserRepository userRepository;

    @MockBean
    private RefreshTokenRepository refreshTokenRepository;

    @MockBean
    private EmailChangeTokenRepository emailChangeTokenRepository;

    @MockBean
    private EmailSender emailSender;

    private AppUser existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new AppUser(
                "alice@example.com",
                passwordEncoder.encode("secret123"),
                "Alice",
                "UTC"
        );
        // Give the in-memory user a stable id so refresh-token rows can reference it.
        setUserId(existingUser, UUID.randomUUID());

        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(existingUser));
        when(userRepository.findById(existingUser.getId()))
                .thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail("new@example.com"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(AppUser.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // --- Register ---

    @Test
    void register_newEmail_returns201WithAccessTokenAndRefreshCookie() throws Exception {
        RegisterRequest req = new RegisterRequest("new@example.com", "password1", "New User", "America/New_York");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        Cookie refreshCookie = result.getResponse().getCookie(AuthService.REFRESH_COOKIE_NAME);
        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie.isHttpOnly()).isTrue();
        assertThat(refreshCookie.getSecure()).isTrue();
        assertThat(refreshCookie.getValue()).isNotBlank();
    }

    @Test
    void register_existingEmail_returns409() throws Exception {
        RegisterRequest req = new RegisterRequest("alice@example.com", "password1", "Dupe", "UTC");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    // --- Login ---

    @Test
    void login_validCredentials_returns200WithAccessTokenAndRefreshCookie() throws Exception {
        LoginRequest req = new LoginRequest("alice@example.com", "secret123");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        Cookie refreshCookie = result.getResponse().getCookie(AuthService.REFRESH_COOKIE_NAME);
        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie.isHttpOnly()).isTrue();
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        LoginRequest req = new LoginRequest("alice@example.com", "wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_unknownEmail_returns401() throws Exception {
        LoginRequest req = new LoginRequest("nobody@example.com", "secret");

        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // --- Refresh ---

    @Test
    void refresh_validCookie_returns200WithNewAccessTokenAndRotatedCookie() throws Exception {
        String rawToken = "raw-refresh-token-value-123";
        RefreshToken stored = new RefreshToken(
                existingUser.getId(),
                AuthService.sha256(rawToken),
                UUID.randomUUID(),
                null,
                Instant.now().plusSeconds(3600)
        );
        when(refreshTokenRepository.findByTokenHash(AuthService.sha256(rawToken)))
                .thenReturn(Optional.of(stored));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie(AuthService.REFRESH_COOKIE_NAME, rawToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        Cookie newRefreshCookie = result.getResponse().getCookie(AuthService.REFRESH_COOKIE_NAME);
        assertThat(newRefreshCookie).isNotNull();
        assertThat(newRefreshCookie.getValue()).isNotBlank();
        // Rotation: the cookie value must change.
        assertThat(newRefreshCookie.getValue()).isNotEqualTo(rawToken);
        // The old row was revoked in place during rotation.
        assertThat(stored.isRevoked()).isTrue();
    }

    @Test
    void refresh_missingCookie_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_unknownCookie_returns401() throws Exception {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie(AuthService.REFRESH_COOKIE_NAME, "ghost")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_revokedCookie_returns401_andRevokesChain() throws Exception {
        String rawToken = "already-rotated";
        RefreshToken stored = new RefreshToken(
                existingUser.getId(),
                AuthService.sha256(rawToken),
                UUID.randomUUID(),
                null,
                Instant.now().plusSeconds(3600)
        );
        stored.revoke(Instant.now().minusSeconds(60));
        when(refreshTokenRepository.findByTokenHash(AuthService.sha256(rawToken)))
                .thenReturn(Optional.of(stored));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie(AuthService.REFRESH_COOKIE_NAME, rawToken)))
                .andExpect(status().isUnauthorized());

        // Reuse detection: every active token for the user is revoked, not just this one.
        verify(refreshTokenRepository).revokeAllActiveForUser(eq(existingUser.getId()), any());
    }

    @Test
    void refresh_expiredCookie_returns401() throws Exception {
        String rawToken = "expired-token";
        RefreshToken stored = new RefreshToken(
                existingUser.getId(),
                AuthService.sha256(rawToken),
                UUID.randomUUID(),
                null,
                Instant.now().minusSeconds(60)
        );
        when(refreshTokenRepository.findByTokenHash(AuthService.sha256(rawToken)))
                .thenReturn(Optional.of(stored));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie(AuthService.REFRESH_COOKIE_NAME, rawToken)))
                .andExpect(status().isUnauthorized());
    }

    // --- Logout ---

    @Test
    void logout_revokesPresentedRefreshToken_andClearsCookie() throws Exception {
        String rawToken = "logout-target";
        RefreshToken stored = new RefreshToken(
                existingUser.getId(),
                AuthService.sha256(rawToken),
                UUID.randomUUID(),
                null,
                Instant.now().plusSeconds(3600)
        );
        when(refreshTokenRepository.findByTokenHash(AuthService.sha256(rawToken)))
                .thenReturn(Optional.of(stored));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new Cookie(AuthService.REFRESH_COOKIE_NAME, rawToken)))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(stored.isRevoked()).isTrue();
        Cookie cleared = result.getResponse().getCookie(AuthService.REFRESH_COOKIE_NAME);
        assertThat(cleared).isNotNull();
        assertThat(cleared.getMaxAge()).isZero();
    }

    @Test
    void logout_withoutCookie_stillReturns204() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent());
    }

    // --- Confirm email change ---

    @Test
    void confirmEmailChange_validToken_switchesEmailAndRevokesSessions() throws Exception {
        String rawToken = "email-change-token";
        EmailChangeToken token = new EmailChangeToken(
                existingUser.getId(), "new@example.com",
                AuthService.sha256(rawToken), Instant.now().plusSeconds(3600));
        when(emailChangeTokenRepository.findByTokenHash(AuthService.sha256(rawToken)))
                .thenReturn(Optional.of(token));

        var req = new ConfirmEmailChangeRequest(rawToken);
        mockMvc.perform(post("/api/v1/auth/email/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@example.com"));

        assertThat(existingUser.getEmail()).isEqualTo("new@example.com");
        assertThat(token.isConsumed()).isTrue();
        verify(refreshTokenRepository).revokeAllActiveForUser(eq(existingUser.getId()), any());
    }

    @Test
    void confirmEmailChange_unknownToken_returns400() throws Exception {
        when(emailChangeTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        var req = new ConfirmEmailChangeRequest("ghost");
        mockMvc.perform(post("/api/v1/auth/email/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void confirmEmailChange_expiredToken_returns400() throws Exception {
        String rawToken = "expired-change";
        EmailChangeToken token = new EmailChangeToken(
                existingUser.getId(), "new@example.com",
                AuthService.sha256(rawToken), Instant.now().minusSeconds(60));
        when(emailChangeTokenRepository.findByTokenHash(AuthService.sha256(rawToken)))
                .thenReturn(Optional.of(token));

        var req = new ConfirmEmailChangeRequest(rawToken);
        mockMvc.perform(post("/api/v1/auth/email/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
        assertThat(existingUser.getEmail()).isEqualTo("alice@example.com");
    }

    // --- Protected endpoints ---

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/tasks"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withValidToken_isNotRejectedAs401() throws Exception {
        String accessToken = jwtService.generateAccessToken("alice@example.com", AppUser.Role.USER);

        // A non-existent endpoint with a valid token should get 404 (not 401)
        MvcResult result = mockMvc.perform(post("/api/v1/tasks")
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(404);
    }

    /** Bypass JPA's normal id assignment so test users can be referenced by id. */
    private static void setUserId(AppUser user, UUID id) {
        try {
            Field f = AppUser.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(user, id);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }
}
