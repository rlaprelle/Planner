package com.echel.planner.backend.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.echel.planner.backend.admin.dto.AdminDeferredItemRequest;
import com.echel.planner.backend.admin.dto.AdminDeferredItemResponse;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
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

@WebMvcTest(AdminDeferredItemController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthFilter.class})
class AdminDeferredItemControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminDeferredItemService adminDeferredItemService;

    @MockBean
    private AppUserRepository appUserRepository;

    private UUID itemId;
    private UUID userId;
    private AdminDeferredItemResponse sampleResponse;

    @BeforeEach
    void setUp() {
        itemId = UUID.randomUUID();
        userId = UUID.randomUUID();
        Instant now = Instant.now();

        sampleResponse = new AdminDeferredItemResponse(
                itemId,
                userId,
                "alice@example.com",
                "Buy groceries",
                false,
                now,
                null,
                null,
                null,
                LocalDate.of(2026, 4, 5),
                0,
                now,
                now
        );
    }

    @Test
    void listAll_returns200WithArray() throws Exception {
        when(adminDeferredItemService.listAll()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/admin/deferred-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(itemId.toString()))
                .andExpect(jsonPath("$[0].rawText").value("Buy groceries"));
    }

    @Test
    void create_returns201() throws Exception {
        AdminDeferredItemRequest req = new AdminDeferredItemRequest(
                userId,
                "Buy groceries",
                false,
                null,
                null,
                LocalDate.of(2026, 4, 5),
                0
        );

        when(adminDeferredItemService.create(any(AdminDeferredItemRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/admin/deferred-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(itemId.toString()))
                .andExpect(jsonPath("$.rawText").value("Buy groceries"));
    }

    @Test
    void update_returns200() throws Exception {
        AdminDeferredItemRequest req = new AdminDeferredItemRequest(
                userId,
                "Buy groceries updated",
                true,
                null,
                null,
                null,
                1
        );

        AdminDeferredItemResponse updated = new AdminDeferredItemResponse(
                itemId,
                userId,
                "alice@example.com",
                "Buy groceries updated",
                true,
                Instant.now(),
                Instant.now(),
                null,
                null,
                null,
                1,
                Instant.now(),
                Instant.now()
        );

        when(adminDeferredItemService.update(eq(itemId), any(AdminDeferredItemRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/v1/admin/deferred-items/{id}", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(itemId.toString()))
                .andExpect(jsonPath("$.rawText").value("Buy groceries updated"))
                .andExpect(jsonPath("$.isProcessed").value(true));
    }

    @Test
    void delete_returns204() throws Exception {
        doNothing().when(adminDeferredItemService).delete(itemId);

        mockMvc.perform(delete("/api/v1/admin/deferred-items/{id}", itemId))
                .andExpect(status().isNoContent());
    }
}
