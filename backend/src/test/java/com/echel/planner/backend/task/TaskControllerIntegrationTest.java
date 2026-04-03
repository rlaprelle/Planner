package com.echel.planner.backend.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.auth.JwtAuthFilter;
import com.echel.planner.backend.auth.JwtService;
import com.echel.planner.backend.auth.SecurityConfig;
import com.echel.planner.backend.task.dto.TaskCreateRequest;
import com.echel.planner.backend.task.dto.TaskResponse;
import com.echel.planner.backend.task.dto.TaskStatusRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthFilter.class})
class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private TaskService taskService;

    @MockBean
    private AppUserRepository appUserRepository;

    private AppUser testUser;
    private String accessToken;
    private UUID projectId;
    private UUID taskId;
    private TaskResponse sampleTask;

    @BeforeEach
    void setUp() {
        testUser = new AppUser("alice@example.com", "hashed", "Alice", "UTC");
        accessToken = jwtService.generateAccessToken("alice@example.com");

        projectId = UUID.randomUUID();
        taskId = UUID.randomUUID();

        Instant now = Instant.now();
        sampleTask = new TaskResponse(
                taskId,
                projectId,
                "My Project",
                "#6B7280",
                testUser.getId(),
                "Write unit tests",
                "Cover all endpoints",
                null,
                TaskStatus.TODO,
                (short) 0,
                (short) 3,
                null,
                EnergyLevel.MEDIUM,
                null,
                0,
                null,
                DeadlineGroup.NO_DEADLINE,
                null,
                null,
                now,
                now,
                List.of()
        );

        when(appUserRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(testUser));
    }

    // --- POST /api/v1/projects/{projectId}/tasks ---

    @Test
    void createTask_authenticated_returns201WithTask() throws Exception {
        TaskCreateRequest req = new TaskCreateRequest(
                "Write unit tests",
                "Cover all endpoints",
                null,
                TaskStatus.TODO,
                null,
                (short) 3,
                null,
                EnergyLevel.MEDIUM,
                null,
                null
        );

        when(taskService.create(any(AppUser.class), eq(projectId), any(TaskCreateRequest.class)))
                .thenReturn(sampleTask);

        mockMvc.perform(post("/api/v1/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(taskId.toString()))
                .andExpect(jsonPath("$.title").value("Write unit tests"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    // --- GET /api/v1/projects/{projectId}/tasks ---

    @Test
    void listTopLevelTasks_authenticated_returns200WithArray() throws Exception {
        when(taskService.listTopLevel(any(AppUser.class), eq(projectId)))
                .thenReturn(List.of(sampleTask));

        mockMvc.perform(get("/api/v1/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(taskId.toString()))
                .andExpect(jsonPath("$[0].title").value("Write unit tests"));
    }

    // --- GET /api/v1/tasks/{id} ---

    @Test
    void getTask_authenticated_returns200WithTask() throws Exception {
        when(taskService.get(any(AppUser.class), eq(taskId)))
                .thenReturn(sampleTask);

        mockMvc.perform(get("/api/v1/tasks/{id}", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId.toString()))
                .andExpect(jsonPath("$.title").value("Write unit tests"))
                .andExpect(jsonPath("$.projectId").value(projectId.toString()));
    }

    // --- PATCH /api/v1/tasks/{id}/status ---

    @Test
    void changeStatus_authenticated_returns200WithUpdatedStatus() throws Exception {
        Instant now = Instant.now();
        TaskResponse completedTask = new TaskResponse(
                taskId,
                projectId,
                "My Project",
                "#6B7280",
                testUser.getId(),
                "Write unit tests",
                "Cover all endpoints",
                null,
                TaskStatus.DONE,
                (short) 0,
                (short) 3,
                null,
                EnergyLevel.MEDIUM,
                null,
                0,
                null,
                DeadlineGroup.NO_DEADLINE,
                null,
                now,
                now,
                now,
                List.of()
        );

        TaskStatusRequest req = new TaskStatusRequest(TaskStatus.DONE);

        when(taskService.changeStatus(any(AppUser.class), eq(taskId), any(TaskStatusRequest.class)))
                .thenReturn(completedTask);

        mockMvc.perform(patch("/api/v1/tasks/{id}/status", taskId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId.toString()))
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
    }

    // --- PATCH /api/v1/tasks/{id}/archive ---

    @Test
    void archiveTask_authenticated_returns200WithArchivedAt() throws Exception {
        Instant now = Instant.now();
        TaskResponse archivedTask = new TaskResponse(
                taskId,
                projectId,
                "My Project",
                "#6B7280",
                testUser.getId(),
                "Write unit tests",
                "Cover all endpoints",
                null,
                TaskStatus.TODO,
                (short) 0,
                (short) 3,
                null,
                EnergyLevel.MEDIUM,
                null,
                0,
                null,
                DeadlineGroup.NO_DEADLINE,
                now,
                null,
                now,
                now,
                List.of()
        );

        when(taskService.archive(any(AppUser.class), eq(taskId)))
                .thenReturn(archivedTask);

        mockMvc.perform(patch("/api/v1/tasks/{id}/archive", taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId.toString()))
                .andExpect(jsonPath("$.archivedAt").isNotEmpty());
    }

    // --- Unauthenticated requests ---

    @Test
    void anyEndpoint_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/projects/{projectId}/tasks", projectId))
                .andExpect(status().isUnauthorized());
    }
}
