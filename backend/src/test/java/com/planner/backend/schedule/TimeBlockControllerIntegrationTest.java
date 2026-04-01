package com.planner.backend.schedule;

import com.planner.backend.auth.AppUser;
import com.planner.backend.auth.AppUserRepository;
import com.planner.backend.auth.JwtAuthFilter;
import com.planner.backend.auth.JwtService;
import com.planner.backend.auth.SecurityConfig;
import com.planner.backend.schedule.dto.TimeBlockResponse;
import com.planner.backend.task.TaskStatus;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
                UUID.randomUUID(), "Work", "#6366f1", TaskStatus.IN_PROGRESS, (short) 2)
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
}
