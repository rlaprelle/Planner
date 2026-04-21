package com.echel.planner.backend.stats;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.auth.JwtAuthFilter;
import com.echel.planner.backend.auth.JwtService;
import com.echel.planner.backend.auth.SecurityConfig;
import com.echel.planner.backend.stats.dto.DashboardResponse;
import com.echel.planner.backend.stats.dto.WeeklySummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
        accessToken = jwtService.generateAccessToken("alice@example.com", AppUser.Role.USER);
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

    @Test
    void dashboard_returnsAggregatedData() throws Exception {
        DashboardResponse response = new DashboardResponse(
                4, 1, 7,
                List.of(new DashboardResponse.DeadlineSummary(
                        java.util.UUID.randomUUID(), "Fix login", "Auth", "#6366f1",
                        java.time.LocalDate.of(2026, 3, 31), "TODAY")),
                3,
                List.of());

        when(statsService.getDashboard(any(AppUser.class))).thenReturn(response);

        mockMvc.perform(get("/api/v1/stats/dashboard")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayBlockCount").value(4))
                .andExpect(jsonPath("$.streakDays").value(7))
                .andExpect(jsonPath("$.upcomingDeadlines[0].taskTitle").value("Fix login"))
                .andExpect(jsonPath("$.deferredItemCount").value(3));
    }

    @Test
    void dashboard_serializesCelebrationTasks() throws Exception {
        DashboardResponse response = new DashboardResponse(
                0, 0, 0, List.of(), 0,
                List.of(new DashboardResponse.CelebrationTask(
                        UUID.randomUUID(), "Big task", "Work", CelebrationReason.HIGH_COMPLEXITY, "High complexity task")));

        when(statsService.getDashboard(any(AppUser.class))).thenReturn(response);

        mockMvc.perform(get("/api/v1/stats/dashboard")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.celebrationTasks[0].taskTitle").value("Big task"))
                .andExpect(jsonPath("$.celebrationTasks[0].reason").value("High complexity task"));
    }

    @Test
    void dashboard_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/stats/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void weeklySummary_returnsAggregatedData() throws Exception {
        WeeklySummaryResponse response = new WeeklySummaryResponse(5, 21, 300, 3, "improving", "steady", true);
        when(statsService.getWeeklySummary(any(AppUser.class))).thenReturn(response);

        mockMvc.perform(get("/api/v1/stats/weekly-summary")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasksCompleted").value(5))
                .andExpect(jsonPath("$.totalPoints").value(21))
                .andExpect(jsonPath("$.totalFocusMinutes").value(300))
                .andExpect(jsonPath("$.streakDays").value(3))
                .andExpect(jsonPath("$.energyTrend").value("improving"))
                .andExpect(jsonPath("$.moodTrend").value("steady"))
                .andExpect(jsonPath("$.hasActivity").value(true));
    }

    @Test
    void weeklySummary_emptyState() throws Exception {
        WeeklySummaryResponse response = new WeeklySummaryResponse(0, 0, 0, 0, null, null, false);
        when(statsService.getWeeklySummary(any(AppUser.class))).thenReturn(response);

        mockMvc.perform(get("/api/v1/stats/weekly-summary")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasksCompleted").value(0))
                .andExpect(jsonPath("$.hasActivity").value(false))
                .andExpect(jsonPath("$.energyTrend").doesNotExist())
                .andExpect(jsonPath("$.moodTrend").doesNotExist());
    }

    @Test
    void weeklySummary_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/stats/weekly-summary"))
                .andExpect(status().isUnauthorized());
    }
}
