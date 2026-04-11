package com.echel.planner.backend.schedule;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.auth.JwtAuthFilter;
import com.echel.planner.backend.auth.JwtService;
import com.echel.planner.backend.auth.SecurityConfig;
import com.echel.planner.backend.schedule.dto.TimeBlockResponse;
import com.echel.planner.backend.task.TaskStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import com.echel.planner.backend.schedule.dto.ExtendRequest;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TimeBlockController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthFilter.class, ScheduleExceptionHandler.class})
class TimeBlockControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtService jwtService;

    @MockBean AppUserRepository userRepository;
    @MockBean ScheduleService scheduleService;

    private AppUser user;
    private String token;
    private UUID blockId;

    @BeforeEach
    void setUp() {
        user = new AppUser("alice@example.com", "hash", "Alice", "UTC");
        token = jwtService.generateAccessToken("alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        blockId = UUID.randomUUID();
    }

    @Test
    void start_returnsUpdatedBlock() throws Exception {
        var response = new TimeBlockResponse(
            blockId, LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0), 0,
            Instant.now(), null, false,
            new TimeBlockResponse.TaskSummary(UUID.randomUUID(), "Write tests",
                UUID.randomUUID(), "Work", "#6366f1", TaskStatus.OPEN, (short) 2),
            null
        );
        when(scheduleService.startBlock(any(AppUser.class), any(UUID.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/time-blocks/{id}/start", blockId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actualStart").isNotEmpty())
            .andExpect(jsonPath("$.task.title").value("Write tests"));
    }

    @Test
    void start_noAuth_returns401() throws Exception {
        mockMvc.perform(patch("/api/v1/time-blocks/{id}/start", blockId))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void complete_returnsUpdatedBlock() throws Exception {
        var response = new TimeBlockResponse(
            blockId, LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0), 0,
            Instant.now().minusSeconds(3600), Instant.now(), true,
            new TimeBlockResponse.TaskSummary(UUID.randomUUID(), "Write tests",
                UUID.randomUUID(), "Work", "#6366f1", TaskStatus.COMPLETED, (short) 2),
            null
        );
        when(scheduleService.completeBlock(any(AppUser.class), any(UUID.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/time-blocks/{id}/complete", blockId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.wasCompleted").value(true))
            .andExpect(jsonPath("$.actualEnd").isNotEmpty())
            .andExpect(jsonPath("$.task.status").value("COMPLETED"));
    }

    @Test
    void doneForNow_returnsUpdatedBlock() throws Exception {
        var response = new TimeBlockResponse(
            blockId, LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0), 0,
            Instant.now().minusSeconds(1800), Instant.now(), false,
            new TimeBlockResponse.TaskSummary(UUID.randomUUID(), "Write tests",
                UUID.randomUUID(), "Work", "#6366f1", TaskStatus.OPEN, (short) 2),
            null
        );
        when(scheduleService.doneForNow(any(AppUser.class), any(UUID.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/time-blocks/{id}/done-for-now", blockId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.wasCompleted").value(false))
            .andExpect(jsonPath("$.actualEnd").isNotEmpty())
            .andExpect(jsonPath("$.task.status").value("OPEN"));
    }

    @Test
    void extend_createsNewBlock() throws Exception {
        UUID newBlockId = UUID.randomUUID();
        var response = new TimeBlockResponse(
            newBlockId, LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(10, 30), 1,
            null, null, false,
            new TimeBlockResponse.TaskSummary(UUID.randomUUID(), "Write tests",
                UUID.randomUUID(), "Work", "#6366f1", TaskStatus.OPEN, (short) 2),
            null
        );
        when(scheduleService.extendBlock(any(AppUser.class), any(UUID.class), any(Integer.class)))
            .thenReturn(response);

        mockMvc.perform(post("/api/v1/time-blocks/{id}/extend", blockId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ExtendRequest(30))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.startTime").value("10:00:00"))
            .andExpect(jsonPath("$.endTime").value("10:30:00"));
    }
}
