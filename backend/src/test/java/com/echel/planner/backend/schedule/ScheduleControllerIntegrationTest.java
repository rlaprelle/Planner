package com.planner.backend.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planner.backend.auth.AppUser;
import com.planner.backend.auth.AppUserRepository;
import com.planner.backend.auth.JwtAuthFilter;
import com.planner.backend.auth.JwtService;
import com.planner.backend.auth.SecurityConfig;
import com.planner.backend.schedule.dto.SavePlanRequest;
import com.planner.backend.schedule.dto.TimeBlockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScheduleController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthFilter.class, ScheduleExceptionHandler.class})
class ScheduleControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtService jwtService;

    @MockBean AppUserRepository userRepository;
    @MockBean ScheduleService scheduleService;

    private AppUser user;
    private String token;

    @BeforeEach
    void setUp() {
        user = new AppUser("alice@example.com", "hash", "Alice", "UTC");
        token = jwtService.generateAccessToken("alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
    }

    @Test
    void getToday_returnsBlocks() throws Exception {
        UUID taskId = UUID.randomUUID();
        TimeBlockResponse.TaskSummary summary = new TimeBlockResponse.TaskSummary(
                taskId, "Fix login", UUID.randomUUID(), "Auth", "#6366f1",
                com.planner.backend.task.TaskStatus.TODO, (short) 3);
        TimeBlockResponse block = new TimeBlockResponse(
                UUID.randomUUID(), LocalDate.of(2026, 3, 31),
                LocalTime.of(9, 0), LocalTime.of(10, 0), 0, null, null, false, summary);

        when(scheduleService.getToday(any(AppUser.class))).thenReturn(List.of(block));

        mockMvc.perform(get("/api/v1/schedule/today")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$[0].task.title").value("Fix login"));
    }

    @Test
    void getToday_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/schedule/today"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void savePlan_validRequest_returns200WithBlocks() throws Exception {
        UUID taskId = UUID.randomUUID();
        SavePlanRequest request = new SavePlanRequest(
                LocalDate.of(2026, 3, 31),
                List.of(new SavePlanRequest.BlockEntry(taskId, LocalTime.of(9, 0), LocalTime.of(10, 0))));

        TimeBlockResponse.TaskSummary summary = new TimeBlockResponse.TaskSummary(
                taskId, "Fix login", UUID.randomUUID(), "Auth", "#6366f1",
                com.planner.backend.task.TaskStatus.TODO, (short) 3);
        TimeBlockResponse block = new TimeBlockResponse(
                UUID.randomUUID(), LocalDate.of(2026, 3, 31),
                LocalTime.of(9, 0), LocalTime.of(10, 0), 0, null, null, false, summary);

        when(scheduleService.savePlan(any(AppUser.class), any(SavePlanRequest.class)))
                .thenReturn(List.of(block));

        mockMvc.perform(post("/api/v1/schedule/today/plan")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].task.title").value("Fix login"));
    }

    @Test
    void savePlan_validationError_returns422() throws Exception {
        UUID taskId = UUID.randomUUID();
        SavePlanRequest request = new SavePlanRequest(
                LocalDate.of(2026, 3, 31),
                List.of(new SavePlanRequest.BlockEntry(taskId, LocalTime.of(9, 0), LocalTime.of(10, 0))));

        when(scheduleService.savePlan(any(AppUser.class), any(SavePlanRequest.class)))
                .thenThrow(new ScheduleService.ScheduleValidationException("Blocks must not overlap"));

        mockMvc.perform(post("/api/v1/schedule/today/plan")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }
}
