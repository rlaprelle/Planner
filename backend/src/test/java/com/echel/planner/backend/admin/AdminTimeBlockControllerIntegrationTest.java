package com.echel.planner.backend.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.echel.planner.backend.admin.dto.AdminTimeBlockRequest;
import com.echel.planner.backend.admin.dto.AdminTimeBlockResponse;
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

@WebMvcTest(AdminTimeBlockController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthFilter.class})
@WithMockUser(roles = "ADMIN")
class AdminTimeBlockControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminTimeBlockService adminTimeBlockService;

    @MockBean
    private AppUserRepository appUserRepository;

    private UUID timeBlockId;
    private UUID userId;
    private UUID taskId;
    private AdminTimeBlockResponse sampleResponse;

    @BeforeEach
    void setUp() {
        timeBlockId = UUID.randomUUID();
        userId = UUID.randomUUID();
        taskId = UUID.randomUUID();
        Instant now = Instant.now();

        sampleResponse = new AdminTimeBlockResponse(
                timeBlockId,
                userId,
                "alice@example.com",
                LocalDate.of(2026, 4, 2),
                taskId,
                "Write tests",
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                1,
                null,
                null,
                false
        );
    }

    @Test
    void listAll_returns200WithArray() throws Exception {
        when(adminTimeBlockService.listAll()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/admin/time-blocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(timeBlockId.toString()))
                .andExpect(jsonPath("$[0].taskTitle").value("Write tests"));
    }

    @Test
    void create_returns201() throws Exception {
        AdminTimeBlockRequest req = new AdminTimeBlockRequest(
                userId,
                LocalDate.of(2026, 4, 2),
                taskId,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                1,
                false
        );

        when(adminTimeBlockService.create(any(AdminTimeBlockRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/admin/time-blocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(timeBlockId.toString()))
                .andExpect(jsonPath("$.startTime").value("09:00:00"))
                .andExpect(jsonPath("$.endTime").value("10:00:00"));
    }

    @Test
    void update_returns200() throws Exception {
        AdminTimeBlockRequest req = new AdminTimeBlockRequest(
                userId,
                LocalDate.of(2026, 4, 2),
                taskId,
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                2,
                true
        );

        AdminTimeBlockResponse updated = new AdminTimeBlockResponse(
                timeBlockId,
                userId,
                "alice@example.com",
                LocalDate.of(2026, 4, 2),
                taskId,
                "Write tests",
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                2,
                null,
                null,
                true
        );

        when(adminTimeBlockService.update(eq(timeBlockId), any(AdminTimeBlockRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/v1/admin/time-blocks/{id}", timeBlockId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(timeBlockId.toString()))
                .andExpect(jsonPath("$.startTime").value("10:00:00"))
                .andExpect(jsonPath("$.wasCompleted").value(true));
    }

    @Test
    void delete_returns204() throws Exception {
        doNothing().when(adminTimeBlockService).delete(timeBlockId);

        mockMvc.perform(delete("/api/v1/admin/time-blocks/{id}", timeBlockId))
                .andExpect(status().isNoContent());
    }
}
