package com.echel.planner.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.echel.planner.backend.auth.dto.LoginRequest;
import com.echel.planner.backend.auth.dto.RegisterRequest;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthFilter.class, AuthService.class, AuthExceptionHandler.class})
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

    private AppUser existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new AppUser(
                "alice@example.com",
                passwordEncoder.encode("secret123"),
                "Alice",
                "UTC"
        );

        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(existingUser));
        when(userRepository.findByEmail("new@example.com"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(AppUser.class)))
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
    void refresh_validCookie_returns200WithNewAccessToken() throws Exception {
        String refreshToken = jwtService.generateRefreshToken("alice@example.com", AppUser.Role.USER);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie(AuthService.REFRESH_COOKIE_NAME, refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        Cookie newRefreshCookie = result.getResponse().getCookie(AuthService.REFRESH_COOKIE_NAME);
        assertThat(newRefreshCookie).isNotNull();
        assertThat(newRefreshCookie.getValue()).isNotBlank();
    }

    @Test
    void refresh_missingCookie_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized());
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
}
