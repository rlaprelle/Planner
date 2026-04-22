package com.echel.planner.backend.project;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.auth.JwtAuthFilter;
import com.echel.planner.backend.auth.JwtService;
import com.echel.planner.backend.auth.SecurityConfig;
import com.echel.planner.backend.project.dto.ProjectCreateRequest;
import com.echel.planner.backend.project.dto.ProjectResponse;
import com.echel.planner.backend.project.dto.ProjectUpdateRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthFilter.class})
class ProjectControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private ProjectService projectService;

    @MockBean
    private AppUserRepository appUserRepository;

    private AppUser testUser;
    private String accessToken;

    @BeforeEach
    void setUp() {
        testUser = new AppUser(
                "alice@example.com",
                "hashed-password",
                "Alice",
                "UTC"
        );

        when(appUserRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(testUser));

        accessToken = jwtService.generateAccessToken("alice@example.com", AppUser.Role.USER);
    }

    // --- POST /api/v1/projects ---

    @Test
    void createProject_validRequest_returns201WithProject() throws Exception {
        UUID projectId = UUID.randomUUID();
        Instant now = Instant.now();

        ProjectCreateRequest request = new ProjectCreateRequest(
                "My Project", "A description", "#7C3AED", "star", 0
        );

        ProjectResponse response = new ProjectResponse(
                projectId, "My Project", "A description", "#7C3AED", "star",
                true, 0, null, now, now
        );

        when(projectService.create(any(AppUser.class), any(ProjectCreateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(projectId.toString()))
                .andExpect(jsonPath("$.name").value("My Project"))
                .andExpect(jsonPath("$.description").value("A description"))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    // --- GET /api/v1/projects ---

    @Test
    void listProjects_authenticated_returns200WithArray() throws Exception {
        Instant now = Instant.now();

        List<ProjectResponse> projects = List.of(
                new ProjectResponse(UUID.randomUUID(), "Project A", null, "#7C3AED", null, true, 0, null, now, now),
                new ProjectResponse(UUID.randomUUID(), "Project B", "Desc", "#3B82F6", "bolt", true, 1, null, now, now)
        );

        when(projectService.listActive(any(AppUser.class))).thenReturn(projects);

        mockMvc.perform(get("/api/v1/projects")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Project A"))
                .andExpect(jsonPath("$[1].name").value("Project B"));
    }

    // --- GET /api/v1/projects/{id} ---

    @Test
    void getProject_existingId_returns200WithProject() throws Exception {
        UUID projectId = UUID.randomUUID();
        Instant now = Instant.now();

        ProjectResponse response = new ProjectResponse(
                projectId, "My Project", "A description", "#7C3AED", "star",
                true, 0, null, now, now
        );

        when(projectService.get(any(AppUser.class), eq(projectId))).thenReturn(response);

        mockMvc.perform(get("/api/v1/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId.toString()))
                .andExpect(jsonPath("$.name").value("My Project"));
    }

    // --- PUT /api/v1/projects/{id} ---

    @Test
    void updateProject_validRequest_returns200WithUpdatedProject() throws Exception {
        UUID projectId = UUID.randomUUID();
        Instant now = Instant.now();

        ProjectUpdateRequest request = new ProjectUpdateRequest(
                "Updated Name", "Updated description", "#3B82F6", "bolt", 2
        );

        ProjectResponse response = new ProjectResponse(
                projectId, "Updated Name", "Updated description", "#3B82F6", "bolt",
                true, 2, null, now, now
        );

        when(projectService.update(any(AppUser.class), eq(projectId), any(ProjectUpdateRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId.toString()))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.color").value("#3B82F6"));
    }

    // --- PATCH /api/v1/projects/{id}/archive ---

    @Test
    void archiveProject_existingId_returns200WithIsActiveFalse() throws Exception {
        UUID projectId = UUID.randomUUID();
        Instant now = Instant.now();
        Instant archivedAt = Instant.now();

        ProjectResponse response = new ProjectResponse(
                projectId, "My Project", "A description", "#7C3AED", "star",
                false, 0, archivedAt, now, now
        );

        when(projectService.archive(any(AppUser.class), eq(projectId))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/projects/{id}/archive", projectId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId.toString()))
                .andExpect(jsonPath("$.isActive").value(false))
                .andExpect(jsonPath("$.archivedAt").isNotEmpty());
    }

    // --- Unauthenticated ---

    @Test
    void anyProjectEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isUnauthorized());
    }
}
