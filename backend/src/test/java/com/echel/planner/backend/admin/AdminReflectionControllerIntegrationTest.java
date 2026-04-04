package com.echel.planner.backend.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.echel.planner.backend.admin.dto.AdminReflectionRequest;
import com.echel.planner.backend.admin.dto.AdminReflectionResponse;
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

@WebMvcTest(AdminReflectionController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthFilter.class})
class AdminReflectionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminReflectionService adminReflectionService;

    @MockBean
    private AppUserRepository appUserRepository;

    private UUID reflectionId;
    private UUID userId;
    private AdminReflectionResponse sampleResponse;

    @BeforeEach
    void setUp() {
        reflectionId = UUID.randomUUID();
        userId = UUID.randomUUID();
        Instant now = Instant.now();

        sampleResponse = new AdminReflectionResponse(
                reflectionId,
                userId,
                "alice@example.com",
                LocalDate.of(2026, 4, 1),
                (short) 4,
                (short) 3,
                "Good day overall",
                true,
                now,
                now
        );
    }

    @Test
    void listAll_returns200WithArray() throws Exception {
        when(adminReflectionService.listAll()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/admin/reflections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(reflectionId.toString()))
                .andExpect(jsonPath("$[0].reflectionNotes").value("Good day overall"));
    }

    @Test
    void create_returns201() throws Exception {
        AdminReflectionRequest req = new AdminReflectionRequest(
                userId,
                LocalDate.of(2026, 4, 1),
                (short) 4,
                (short) 3,
                "Good day overall",
                true
        );

        when(adminReflectionService.create(any(AdminReflectionRequest.class)))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/admin/reflections")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(reflectionId.toString()))
                .andExpect(jsonPath("$.energyRating").value(4))
                .andExpect(jsonPath("$.moodRating").value(3));
    }

    @Test
    void update_returns200() throws Exception {
        AdminReflectionRequest req = new AdminReflectionRequest(
                userId,
                LocalDate.of(2026, 4, 1),
                (short) 5,
                (short) 5,
                "Excellent day",
                true
        );

        AdminReflectionResponse updated = new AdminReflectionResponse(
                reflectionId,
                userId,
                "alice@example.com",
                LocalDate.of(2026, 4, 1),
                (short) 5,
                (short) 5,
                "Excellent day",
                true,
                Instant.now(),
                Instant.now()
        );

        when(adminReflectionService.update(eq(reflectionId), any(AdminReflectionRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/v1/admin/reflections/{id}", reflectionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reflectionId.toString()))
                .andExpect(jsonPath("$.energyRating").value(5))
                .andExpect(jsonPath("$.reflectionNotes").value("Excellent day"));
    }

    @Test
    void delete_returns204() throws Exception {
        doNothing().when(adminReflectionService).delete(reflectionId);

        mockMvc.perform(delete("/api/v1/admin/reflections/{id}", reflectionId))
                .andExpect(status().isNoContent());
    }
}
