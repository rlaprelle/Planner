package com.echel.planner.backend.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.echel.planner.backend.admin.dto.AdminEventRequest;
import com.echel.planner.backend.admin.dto.AdminEventResponse;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.auth.JwtAuthFilter;
import com.echel.planner.backend.auth.JwtService;
import com.echel.planner.backend.auth.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminEventController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthFilter.class})
@WithMockUser(roles = "ADMIN")
class AdminEventControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminEventService adminEventService;

    @MockBean
    private AppUserRepository appUserRepository;

    private UUID eventId;
    private UUID userId;
    private UUID projectId;
    private AdminEventResponse sampleResponse;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        userId = UUID.randomUUID();
        projectId = UUID.randomUUID();

        sampleResponse = new AdminEventResponse(
                eventId,
                userId,
                "alice@example.com",
                projectId,
                "My Project",
                "Team Standup",
                "Daily standup meeting",
                "MEDIUM",
                LocalDate.of(2026, 4, 2),
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                null,
                null,
                null
        );
    }

    @Test
    void listAll_returns200WithArray() throws Exception {
        when(adminEventService.listAll()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/admin/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(eventId.toString()))
                .andExpect(jsonPath("$[0].title").value("Team Standup"));
    }

    @Test
    void create_returns201() throws Exception {
        AdminEventRequest req = new AdminEventRequest(
                userId,
                projectId,
                "Team Standup",
                "Daily standup meeting",
                "MEDIUM",
                LocalDate.of(2026, 4, 2),
                LocalTime.of(9, 0),
                LocalTime.of(9, 30)
        );

        when(adminEventService.create(any(AdminEventRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/admin/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(eventId.toString()))
                .andExpect(jsonPath("$.startTime").value("09:00:00"))
                .andExpect(jsonPath("$.endTime").value("09:30:00"));
    }

    @Test
    void update_returns200() throws Exception {
        AdminEventRequest req = new AdminEventRequest(
                userId,
                projectId,
                "Updated Standup",
                "Updated description",
                "HIGH",
                LocalDate.of(2026, 4, 2),
                LocalTime.of(10, 0),
                LocalTime.of(10, 30)
        );

        AdminEventResponse updated = new AdminEventResponse(
                eventId,
                userId,
                "alice@example.com",
                projectId,
                "My Project",
                "Updated Standup",
                "Updated description",
                "HIGH",
                LocalDate.of(2026, 4, 2),
                LocalTime.of(10, 0),
                LocalTime.of(10, 30),
                null,
                null,
                null
        );

        when(adminEventService.update(eq(eventId), any(AdminEventRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/v1/admin/events/{id}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId.toString()))
                .andExpect(jsonPath("$.title").value("Updated Standup"))
                .andExpect(jsonPath("$.energyLevel").value("HIGH"));
    }

    @Test
    void delete_returns204() throws Exception {
        doNothing().when(adminEventService).delete(eventId);

        mockMvc.perform(delete("/api/v1/admin/events/{id}", eventId))
                .andExpect(status().isNoContent());
    }
}
