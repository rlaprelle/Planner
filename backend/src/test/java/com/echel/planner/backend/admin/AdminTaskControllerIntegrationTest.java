package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminTaskRequest;
import com.echel.planner.backend.admin.dto.AdminTaskResponse;
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

@WebMvcTest(AdminTaskController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthFilter.class})
class AdminTaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminTaskService adminTaskService;

    @MockBean
    private AppUserRepository appUserRepository;

    private static final UUID TASK_ID    = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final UUID USER_ID    = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PROJECT_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    private AdminTaskResponse sampleResponse() {
        return new AdminTaskResponse(
                TASK_ID,
                USER_ID,
                "alice@example.com",
                PROJECT_ID,
                "My Project",
                "Write tests",
                "Integration tests for admin controllers",
                null,
                "OPEN",
                (short) 0,
                (short) 3,
                null,
                "MEDIUM",
                null,
                0,
                null,
                null,
                null,
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-01T00:00:00Z")
        );
    }

    @Test
    void listAll_returns200WithArray() throws Exception {
        when(adminTaskService.listAll()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/admin/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(TASK_ID.toString()))
                .andExpect(jsonPath("$[0].title").value("Write tests"));
    }

    @Test
    void create_validRequest_returns201WithTask() throws Exception {
        AdminTaskRequest request = new AdminTaskRequest(
                USER_ID, PROJECT_ID, "Write tests", "Integration tests for admin controllers",
                null, "OPEN", (short) 0, (short) 3, null, "MEDIUM", null, 0, null);
        when(adminTaskService.create(any(AdminTaskRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/admin/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(TASK_ID.toString()))
                .andExpect(jsonPath("$.title").value("Write tests"))
                .andExpect(jsonPath("$.projectName").value("My Project"));
    }

    @Test
    void update_existingId_returns200WithUpdatedTask() throws Exception {
        AdminTaskRequest request = new AdminTaskRequest(
                USER_ID, PROJECT_ID, "Write tests (updated)", null,
                null, "OPEN", (short) 1, (short) 5, null, "HIGH", null, 0, null);
        AdminTaskResponse updated = new AdminTaskResponse(
                TASK_ID,
                USER_ID,
                "alice@example.com",
                PROJECT_ID,
                "My Project",
                "Write tests (updated)",
                null,
                null,
                "OPEN",
                (short) 1,
                (short) 5,
                null,
                "HIGH",
                null,
                0,
                null,
                null,
                null,
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-06-01T00:00:00Z")
        );
        when(adminTaskService.update(eq(TASK_ID), any(AdminTaskRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/admin/tasks/{id}", TASK_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Write tests (updated)"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void delete_existingId_returns204() throws Exception {
        doNothing().when(adminTaskService).delete(TASK_ID);

        mockMvc.perform(delete("/api/v1/admin/tasks/{id}", TASK_ID))
                .andExpect(status().isNoContent());
    }
}
