package com.echel.planner.backend.stats;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.auth.JwtService;
import com.echel.planner.backend.project.Project;
import com.echel.planner.backend.project.ProjectRepository;
import com.echel.planner.backend.reflection.DailyReflection;
import com.echel.planner.backend.reflection.DailyReflectionRepository;
import com.echel.planner.backend.task.Task;
import com.echel.planner.backend.task.TaskRepository;
import com.echel.planner.backend.task.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StatsControllerWeeklySummaryTest {

    @Autowired MockMvc mockMvc;
    @Autowired AppUserRepository userRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired TaskRepository taskRepository;
    @Autowired DailyReflectionRepository reflectionRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    private String token;
    private AppUser user;
    private Project project;

    @BeforeEach
    void setUp() {
        user = new AppUser(
                "weekly-test-" + java.util.UUID.randomUUID() + "@example.com",
                passwordEncoder.encode("password"),
                "Weekly Tester",
                "UTC"
        );
        user = userRepository.save(user);

        project = new Project(user, "Test Project");
        project = projectRepository.save(project);

        token = jwtService.generateAccessToken(user.getEmail());
    }

    @Test
    void returnsEmptyStateWhenNoActivity() throws Exception {
        mockMvc.perform(get("/api/v1/stats/weekly-summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasksCompleted").value(0))
                .andExpect(jsonPath("$.totalPoints").value(0))
                .andExpect(jsonPath("$.totalFocusMinutes").value(0))
                .andExpect(jsonPath("$.hasActivity").value(false));
    }

    @Test
    void countsCompletedTasksInRollingWindow() throws Exception {
        Task t1 = new Task(user, project, "Done today");
        t1.setStatus(TaskStatus.DONE);
        t1.setCompletedAt(Instant.now());
        t1.setPointsEstimate((short) 5);
        t1.setActualMinutes(90);
        taskRepository.save(t1);

        Task t2 = new Task(user, project, "Done 3 days ago");
        t2.setStatus(TaskStatus.DONE);
        t2.setCompletedAt(Instant.now().minus(3, ChronoUnit.DAYS));
        t2.setPointsEstimate((short) 3);
        t2.setActualMinutes(60);
        taskRepository.save(t2);

        Task t3 = new Task(user, project, "Done 10 days ago");
        t3.setStatus(TaskStatus.DONE);
        t3.setCompletedAt(Instant.now().minus(10, ChronoUnit.DAYS));
        t3.setPointsEstimate((short) 8);
        taskRepository.save(t3);

        mockMvc.perform(get("/api/v1/stats/weekly-summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasksCompleted").value(2))
                .andExpect(jsonPath("$.totalPoints").value(8))
                .andExpect(jsonPath("$.totalFocusMinutes").value(150))
                .andExpect(jsonPath("$.hasActivity").value(true));
    }

    @Test
    void computesMoodAndEnergyTrends() throws Exception {
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));

        saveReflection(today.minusDays(5), (short) 2, (short) 2);
        saveReflection(today.minusDays(4), (short) 2, (short) 1);

        saveReflection(today.minusDays(1), (short) 4, (short) 5);
        saveReflection(today, (short) 5, (short) 4);

        mockMvc.perform(get("/api/v1/stats/weekly-summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.energyTrend").value("improving"))
                .andExpect(jsonPath("$.moodTrend").value("improving"));
    }

    @Test
    void returnsNullTrendsWithInsufficientData() throws Exception {
        saveReflection(LocalDate.now(ZoneId.of("UTC")), (short) 3, (short) 3);

        mockMvc.perform(get("/api/v1/stats/weekly-summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.energyTrend").doesNotExist())
                .andExpect(jsonPath("$.moodTrend").doesNotExist());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/stats/weekly-summary"))
                .andExpect(status().isUnauthorized());
    }

    private void saveReflection(LocalDate date, short energy, short mood) {
        DailyReflection r = new DailyReflection(user, date);
        r.setEnergyRating(energy);
        r.setMoodRating(mood);
        r.setFinalized(true);
        reflectionRepository.save(r);
    }
}
