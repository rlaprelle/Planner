package com.planner.backend.deferred;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planner.backend.auth.AppUser;
import com.planner.backend.auth.AppUserRepository;
import com.planner.backend.auth.JwtAuthFilter;
import com.planner.backend.auth.JwtService;
import com.planner.backend.auth.SecurityConfig;
import com.planner.backend.deferred.dto.DeferredItemCreateRequest;
import com.planner.backend.deferred.dto.DeferredItemResponse;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
