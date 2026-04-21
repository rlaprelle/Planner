package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminUserRequest;
import com.echel.planner.backend.admin.dto.AdminUserResponse;
import com.echel.planner.backend.admin.dto.DependentCountResponse;
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

@WebMvcTest(AdminUserController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthFilter.class})
@WithMockUser(roles = "ADMIN")
class AdminUserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminUserService adminUserService;

    @MockBean
    private AppUserRepository appUserRepository;

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private AdminUserResponse sampleResponse() {
        return new AdminUserResponse(
                USER_ID,
                "alice@example.com",
                "Alice",
                "UTC",
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-01-01T00:00:00Z")
        );
    }

    @Test
    void listAll_returns200WithArray() throws Exception {
        when(adminUserService.listAll()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(USER_ID.toString()))
                .andExpect(jsonPath("$[0].email").value("alice@example.com"));
    }

    @Test
    void get_existingId_returns200WithUser() throws Exception {
        when(adminUserService.get(USER_ID)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/admin/users/{id}", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.displayName").value("Alice"));
    }

    @Test
    void create_validRequest_returns201WithUser() throws Exception {
        AdminUserRequest request = new AdminUserRequest(
                "alice@example.com", "password1", "Alice", "UTC");
        when(adminUserService.create(any(AdminUserRequest.class))).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void update_existingId_returns200WithUpdatedUser() throws Exception {
        AdminUserRequest request = new AdminUserRequest(
                "alice-updated@example.com", null, "Alice Updated", "America/New_York");
        AdminUserResponse updated = new AdminUserResponse(
                USER_ID,
                "alice-updated@example.com",
                "Alice Updated",
                "America/New_York",
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-06-01T00:00:00Z")
        );
        when(adminUserService.update(eq(USER_ID), any(AdminUserRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/admin/users/{id}", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice-updated@example.com"))
                .andExpect(jsonPath("$.displayName").value("Alice Updated"));
    }

    @Test
    void getDependents_existingId_returns200WithCounts() throws Exception {
        DependentCountResponse counts = new DependentCountResponse(3L, 12L, 5L, 2L, 8L);
        when(adminUserService.getDependentCounts(USER_ID)).thenReturn(counts);

        mockMvc.perform(get("/api/v1/admin/users/{id}/dependents", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projects").value(3))
                .andExpect(jsonPath("$.tasks").value(12))
                .andExpect(jsonPath("$.deferredItems").value(5))
                .andExpect(jsonPath("$.reflections").value(2))
                .andExpect(jsonPath("$.timeBlocks").value(8));
    }

    @Test
    void delete_existingId_returns204() throws Exception {
        doNothing().when(adminUserService).delete(USER_ID);

        mockMvc.perform(delete("/api/v1/admin/users/{id}", USER_ID))
                .andExpect(status().isNoContent());
    }
}
