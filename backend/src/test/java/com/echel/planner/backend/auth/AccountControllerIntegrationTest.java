package com.echel.planner.backend.auth;

import com.echel.planner.backend.auth.dto.ChangeEmailRequest;
import com.echel.planner.backend.auth.dto.ChangePasswordRequest;
import com.echel.planner.backend.common.GlobalExceptionHandler;
import com.echel.planner.backend.email.EmailSender;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthFilter.class, AuthRateLimitFilter.class,
         AuthService.class, AccountService.class, AuthExceptionHandler.class, GlobalExceptionHandler.class})
class AccountControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockBean private AppUserRepository userRepository;
    @MockBean private RefreshTokenRepository refreshTokenRepository;
    @MockBean private EmailChangeTokenRepository emailChangeTokenRepository;
    @MockBean private EmailSender emailSender;

    private AppUser user;
    private String accessToken;

    @BeforeEach
    void setUp() {
        user = new AppUser("alice@example.com",
                passwordEncoder.encode("secret123"), "Alice", "UTC");
        setUserId(user, UUID.randomUUID());
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
        accessToken = jwtService.generateAccessToken("alice@example.com", AppUser.Role.USER);
    }

    // --- Change password ---

    @Test
    void changePassword_valid_returns200_rotatesSession_andRevokesOthers() throws Exception {
        String oldHash = user.getPasswordHash();
        var req = new ChangePasswordRequest("secret123", "newsecret123");

        MvcResult result = mockMvc.perform(post("/api/v1/user/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        // Password was re-hashed (different from the old hash) and matches the new value.
        assertThat(user.getPasswordHash()).isNotEqualTo(oldHash);
        assertThat(passwordEncoder.matches("newsecret123", user.getPasswordHash())).isTrue();
        // Sign-out-everywhere fired, and a fresh session cookie was issued for this client.
        verify(refreshTokenRepository).revokeAllActiveForUser(eq(user.getId()), any());
        Cookie cookie = result.getResponse().getCookie(AuthService.REFRESH_COOKIE_NAME);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isNotBlank();
    }

    @Test
    void changePassword_wrongCurrent_returns401_andDoesNotChange() throws Exception {
        String oldHash = user.getPasswordHash();
        var req = new ChangePasswordRequest("wrongpass", "newsecret123");

        mockMvc.perform(post("/api/v1/user/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());

        assertThat(user.getPasswordHash()).isEqualTo(oldHash);
        verify(refreshTokenRepository, never()).revokeAllActiveForUser(any(), any());
    }

    @Test
    void changePassword_sameAsCurrent_returns422() throws Exception {
        var req = new ChangePasswordRequest("secret123", "secret123");

        mockMvc.perform(post("/api/v1/user/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void changePassword_tooShortNew_returns400() throws Exception {
        var req = new ChangePasswordRequest("secret123", "short");

        mockMvc.perform(post("/api/v1/user/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changePassword_unauthenticated_returns401() throws Exception {
        var req = new ChangePasswordRequest("secret123", "newsecret123");

        mockMvc.perform(post("/api/v1/user/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // --- Request email change ---

    @Test
    void requestEmailChange_valid_returns202_andSendsVerification() throws Exception {
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        var req = new ChangeEmailRequest("new@example.com", "secret123");

        mockMvc.perform(post("/api/v1/user/email")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isAccepted());

        // Email not switched yet, a token row was persisted, and a link was sent to the NEW address.
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
        verify(emailChangeTokenRepository).save(any(EmailChangeToken.class));
        verify(emailSender).sendEmailChangeVerification(eq("new@example.com"), eq("Alice"), anyString());
    }

    @Test
    void requestEmailChange_wrongPassword_returns401() throws Exception {
        var req = new ChangeEmailRequest("new@example.com", "wrongpass");

        mockMvc.perform(post("/api/v1/user/email")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());

        verify(emailSender, never()).sendEmailChangeVerification(anyString(), anyString(), anyString());
    }

    @Test
    void requestEmailChange_addressTaken_returns409() throws Exception {
        AppUser other = new AppUser("taken@example.com",
                passwordEncoder.encode("x"), "Bob", "UTC");
        when(userRepository.findByEmail("taken@example.com")).thenReturn(Optional.of(other));
        var req = new ChangeEmailRequest("taken@example.com", "secret123");

        mockMvc.perform(post("/api/v1/user/email")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());

        verify(emailSender, never()).sendEmailChangeVerification(anyString(), anyString(), anyString());
    }

    @Test
    void requestEmailChange_sameAsCurrent_returns422() throws Exception {
        var req = new ChangeEmailRequest("alice@example.com", "secret123");

        mockMvc.perform(post("/api/v1/user/email")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity());
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
