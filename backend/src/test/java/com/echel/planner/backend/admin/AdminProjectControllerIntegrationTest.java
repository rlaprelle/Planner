package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminProjectRequest;
import com.echel.planner.backend.admin.dto.AdminProjectResponse;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.auth.JwtAuthFilter;
import com.echel.planner.backend.auth.JwtService;
import com.echel.planner.backend.auth.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminProjectController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthFilter.class})
@WithMockUser(roles = "ADMIN")
class AdminProjectControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminProjectService adminProjectService;

    @MockBean
    private AppUserRepository appUserRepository;

    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID USER_ID    = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private AdminProjectResponse sampleResponse() {
        return new AdminProjectResponse(
                PROJECT_ID,
                USER_ID,
                "alice@example.com",
                "My Project",
                "A description",
                "#7C3AED",
                "star",
                true,
                0,
                null,
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-01T00:00:00Z")
        );
    }

    @Test
    void listAll_returns200WithArray() throws Exception {
        when(adminProjectService.listAll()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/admin/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(PROJECT_ID.toString()))
                .andExpect(jsonPath("$[0].name").value("My Project"));
    }

    @Test
    void create_validRequest_returns201WithProject() throws Exception {
        AdminProjectRequest request = new AdminProjectRequest(
                USER_ID, "My Project", "A description", "#7C3AED", "star", true, 0);
        when(adminProjectService.create(any(AdminProjectRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/admin/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PROJECT_ID.toString()))
                .andExpect(jsonPath("$.name").value("My Project"))
                .andExpect(jsonPath("$.userEmail").value("alice@example.com"));
    }

    @Test
    void update_existingId_returns200WithUpdatedProject() throws Exception {
        AdminProjectRequest request = new AdminProjectRequest(
                USER_ID, "Updated Project", "New description", "#10B981", "check", false, 1);
        AdminProjectResponse updated = new AdminProjectResponse(
                PROJECT_ID,
                USER_ID,
                "alice@example.com",
                "Updated Project",
                "New description",
                "#10B981",
                "check",
                false,
                1,
                null,
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-06-01T00:00:00Z")
        );
        when(adminProjectService.update(eq(PROJECT_ID), any(AdminProjectRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/admin/projects/{id}", PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Project"))
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void delete_existingId_returns204() throws Exception {
        doNothing().when(adminProjectService).delete(PROJECT_ID);

        mockMvc.perform(delete("/api/v1/admin/projects/{id}", PROJECT_ID))
                .andExpect(status().isNoContent());
    }
}
