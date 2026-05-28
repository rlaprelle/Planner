package com.echel.planner.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.echel.planner.backend.auth.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the per-IP rate limit on the public auth endpoints. The limit must
 * trip after {@link AuthRateLimitFilter#CAPACITY} requests within
 * {@link AuthRateLimitFilter#REFILL_PERIOD}.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthFilter.class, AuthService.class, AuthExceptionHandler.class, AuthRateLimitFilter.class})
class AuthRateLimitFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AppUserRepository userRepository;

    @MockBean
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
    }

    @Test
    void login_returnsTooManyRequests_afterCapacityExceeded() throws Exception {
        LoginRequest req = new LoginRequest("attacker@example.com", "guess");
        String body = objectMapper.writeValueAsString(req);

        // First N requests succeed (return 401 because the user doesn't exist —
        // we're testing the filter, not auth outcome).
        for (int i = 0; i < AuthRateLimitFilter.CAPACITY; i++) {
            mockMvc.perform(loginRequest(body, "10.0.0.1"))
                    .andExpect(status().isUnauthorized());
        }

        // The next one from the same IP must be rate-limited.
        ResultActions limited = mockMvc.perform(loginRequest(body, "10.0.0.1"))
                .andExpect(status().isTooManyRequests());

        String retryAfter = limited.andReturn().getResponse().getHeader(HttpHeaders.RETRY_AFTER);
        assertThat(retryAfter).isNotNull();
        assertThat(Integer.parseInt(retryAfter)).isPositive();
    }

    @Test
    void login_fromDifferentIp_isNotAffectedByAnotherIpsBucket() throws Exception {
        LoginRequest req = new LoginRequest("attacker@example.com", "guess");
        String body = objectMapper.writeValueAsString(req);

        for (int i = 0; i < AuthRateLimitFilter.CAPACITY; i++) {
            mockMvc.perform(loginRequest(body, "10.0.0.2"));
        }
        mockMvc.perform(loginRequest(body, "10.0.0.2"))
                .andExpect(status().isTooManyRequests());

        // A request from a different IP must still be allowed through.
        mockMvc.perform(loginRequest(body, "10.0.0.3"))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpServletRequestBuilder loginRequest(String body, String remoteAddr) {
        return post("/api/v1/auth/login")
                .with(request -> {
                    request.setRemoteAddr(remoteAddr);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }
}
