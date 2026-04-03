package com.echel.planner.backend.stats;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.auth.JwtService;
import com.echel.planner.backend.project.Project;
import com.echel.planner.backend.project.ProjectRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StatsControllerCelebrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired AppUserRepository userRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired TaskRepository taskRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    private String token;
    private AppUser user;
    private Project project;

    @BeforeEach
    void setUp() {
        String email = "celebration-" + UUID.randomUUID() + "@example.com";
        user = new AppUser(email, passwordEncoder.encode("password"), "Test User", "UTC");
        user = userRepository.save(user);

        project = new Project(user, "Test Project");
        project = projectRepository.save(project);

        token = jwtService.generateAccessToken(email);
    }

    @Test
    void returnsEmptyCelebrationsWhenNoTasksQualify() throws Exception {
        Task t = new Task(user, project, "Simple task");
        t.setStatus(TaskStatus.DONE);
        t.setCompletedAt(Instant.now());
        t.setPointsEstimate((short) 2);
        taskRepository.save(t);

        mockMvc.perform(get("/api/v1/stats/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.celebrationTasks").isArray())
                .andExpect(jsonPath("$.celebrationTasks").isEmpty());
    }

    @Test
    void celebratesHighPointsTask() throws Exception {
        Task t = new Task(user, project, "Big task");
        t.setStatus(TaskStatus.DONE);
        t.setCompletedAt(Instant.now());
        t.setPointsEstimate((short) 5);
        taskRepository.save(t);

        mockMvc.perform(get("/api/v1/stats/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.celebrationTasks[0].taskTitle").value("Big task"))
                .andExpect(jsonPath("$.celebrationTasks[0].reason").value("High complexity task"));
    }

    @Test
    void celebratesHighTimeTask() throws Exception {
        Task t = new Task(user, project, "Long session");
        t.setStatus(TaskStatus.DONE);
        t.setCompletedAt(Instant.now());
        t.setActualMinutes(150);
        taskRepository.save(t);

        mockMvc.perform(get("/api/v1/stats/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.celebrationTasks[0].taskTitle").value("Long session"))
                .andExpect(jsonPath("$.celebrationTasks[0].reason").value("2h 30m of focused work"));
    }

    @Test
    void capsAtThreeCelebrations() throws Exception {
        for (int i = 1; i <= 5; i++) {
            Task t = new Task(user, project, "Task " + i);
            t.setStatus(TaskStatus.DONE);
            t.setCompletedAt(Instant.now());
            t.setPointsEstimate((short) 5);
            taskRepository.save(t);
        }

        mockMvc.perform(get("/api/v1/stats/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.celebrationTasks.length()").value(3));
    }
}
