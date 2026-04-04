package com.echel.planner.backend.deferred;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.auth.JwtAuthFilter;
import com.echel.planner.backend.auth.JwtService;
import com.echel.planner.backend.auth.SecurityConfig;
import com.echel.planner.backend.deferred.dto.DeferredItemCreateRequest;
import com.echel.planner.backend.deferred.dto.DeferredItemResponse;
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

import com.echel.planner.backend.deferred.dto.ConvertToTaskRequest;
import com.echel.planner.backend.deferred.dto.DeferRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeferredItemController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthFilter.class, DeferredItemExceptionHandler.class})
class DeferredItemControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private AppUserRepository userRepository;

    @MockBean
    private DeferredItemService deferredItemService;

    private AppUser user;
    private String accessToken;

    @BeforeEach
    void setUp() {
        user = new AppUser("alice@example.com", "hash", "Alice", "UTC");
        accessToken = jwtService.generateAccessToken("alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
    }

    // --- POST /api/v1/deferred ---

    @Test
    void create_validRequest_returns201WithItem() throws Exception {
        DeferredItemCreateRequest req = new DeferredItemCreateRequest("Buy oat milk");
        DeferredItemResponse resp = new DeferredItemResponse(
                UUID.randomUUID(), user.getId(), "Buy oat milk",
                false, Instant.now(), null, null, null, null, 0,
                Instant.now(), Instant.now()
        );
        when(deferredItemService.create(any(AppUser.class), any(DeferredItemCreateRequest.class)))
                .thenReturn(resp);

        mockMvc.perform(post("/api/v1/deferred")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rawText").value("Buy oat milk"))
                .andExpect(jsonPath("$.isProcessed").value(false));
    }

    @Test
    void create_noAuth_returns401() throws Exception {
        DeferredItemCreateRequest req = new DeferredItemCreateRequest("Unauth thought");

        mockMvc.perform(post("/api/v1/deferred")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_blankRawText_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/deferred")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rawText\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/v1/deferred ---

    @Test
    void listPending_validAuth_returns200WithItems() throws Exception {
        DeferredItemResponse item = new DeferredItemResponse(
                UUID.randomUUID(), user.getId(), "Call dentist",
                false, Instant.now(), null, null, null, null, 0,
                Instant.now(), Instant.now()
        );
        when(deferredItemService.listPending(any(AppUser.class))).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/deferred")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rawText").value("Call dentist"))
                .andExpect(jsonPath("$[0].isProcessed").value(false));
    }

    @Test
    void listPending_noItems_returns200WithEmptyArray() throws Exception {
        when(deferredItemService.listPending(any(AppUser.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/deferred")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void listPending_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/deferred"))
                .andExpect(status().isUnauthorized());
    }

    // --- POST /api/v1/deferred/{id}/convert ---

    @Test
    void convert_validRequest_returns200WithTask() throws Exception {
        UUID itemId = UUID.randomUUID();
        ConvertToTaskRequest req = new ConvertToTaskRequest(
                UUID.randomUUID(), "Buy oat milk", null, null, null, null);

        com.echel.planner.backend.task.dto.TaskResponse taskResp =
                new com.echel.planner.backend.task.dto.TaskResponse(
                        UUID.randomUUID(), UUID.randomUUID(), "Test Project", "#6366f1",
                        UUID.randomUUID(), "Buy oat milk", null, null,
                        com.echel.planner.backend.task.TaskStatus.TODO,
                        (short) 3, null, null, null, null, 0, null,
                        com.echel.planner.backend.task.DeadlineGroup.NO_DEADLINE,
                        null, null, null, null, java.util.List.of());

        when(deferredItemService.convert(any(AppUser.class), any(UUID.class),
                any(ConvertToTaskRequest.class))).thenReturn(taskResp);

        mockMvc.perform(post("/api/v1/deferred/{id}/convert", itemId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Buy oat milk"));
    }

    @Test
    void convert_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/deferred/{id}/convert", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":\"" + UUID.randomUUID() + "\",\"title\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    // --- POST /api/v1/deferred/{id}/defer ---

    @Test
    void defer_validRequest_returns200WithItem() throws Exception {
        UUID itemId = UUID.randomUUID();
        DeferRequest req = new DeferRequest(DeferRequest.DeferDuration.ONE_DAY);
        DeferredItemResponse resp = new DeferredItemResponse(
                itemId, user.getId(), "Buy oat milk",
                false, Instant.now(), null, null, null,
                java.time.LocalDate.now().plusDays(1), 1,
                Instant.now(), Instant.now());

        when(deferredItemService.defer(any(AppUser.class), any(UUID.class),
                any(DeferRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/deferred/{id}/defer", itemId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deferralCount").value(1));
    }

    @Test
    void defer_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/deferred/{id}/defer", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deferFor\":\"ONE_DAY\"}"))
                .andExpect(status().isUnauthorized());
    }

    // --- PATCH /api/v1/deferred/{id}/dismiss ---

    @Test
    void dismiss_validRequest_returns200() throws Exception {
        UUID itemId = UUID.randomUUID();
        DeferredItemResponse resp = new DeferredItemResponse(
                itemId, user.getId(), "Old thought",
                true, Instant.now(), Instant.now(), null, null, null, 0,
                Instant.now(), Instant.now());

        when(deferredItemService.dismiss(any(AppUser.class), any(UUID.class))).thenReturn(resp);

        mockMvc.perform(patch("/api/v1/deferred/{id}/dismiss", itemId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isProcessed").value(true));
    }

    @Test
    void dismiss_noAuth_returns401() throws Exception {
        mockMvc.perform(patch("/api/v1/deferred/{id}/dismiss", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}
