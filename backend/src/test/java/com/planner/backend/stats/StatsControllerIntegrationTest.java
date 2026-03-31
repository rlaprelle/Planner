package com.planner.backend.stats;

import com.planner.backend.auth.AppUser;
import com.planner.backend.auth.AppUserRepository;
import com.planner.backend.auth.JwtAuthFilter;
import com.planner.backend.auth.JwtService;
import com.planner.backend.auth.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatsController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthFilter.class})
class StatsControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;

    @MockBean AppUserRepository userRepository;
    @MockBean StatsService statsService;

    private AppUser user;
    private String accessToken;

    @BeforeEach
    void setUp() {
        user = new AppUser("alice@example.com", "hash", "Alice", "UTC");
        accessToken = jwtService.generateAccessToken("alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
    }

    @Test
    void streak_returnsCount() throws Exception {
        when(statsService.getStreak(any(AppUser.class))).thenReturn(5);

        mockMvc.perform(get("/api/v1/stats/streak")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.streak").value(5));
    }

    @Test
    void streak_noStreak_returnsZero() throws Exception {
        when(statsService.getStreak(any(AppUser.class))).thenReturn(0);

        mockMvc.perform(get("/api/v1/stats/streak")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.streak").value(0));
    }

    @Test
    void streak_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/stats/streak"))
                .andExpect(status().isUnauthorized());
    }
}
