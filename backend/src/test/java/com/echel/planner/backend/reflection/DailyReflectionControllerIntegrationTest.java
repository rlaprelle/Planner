package com.echel.planner.backend.reflection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.auth.JwtAuthFilter;
import com.echel.planner.backend.auth.JwtService;
import com.echel.planner.backend.auth.SecurityConfig;
import com.echel.planner.backend.reflection.dto.ReflectionRequest;
import com.echel.planner.backend.reflection.dto.ReflectionResponse;
import com.echel.planner.backend.reflection.ReflectionType;
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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DailyReflectionController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthFilter.class})
class DailyReflectionControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtService jwtService;

    @MockBean AppUserRepository userRepository;
    @MockBean DailyReflectionService reflectionService;

    private AppUser user;
    private String accessToken;

    @BeforeEach
    void setUp() {
        user = new AppUser("alice@example.com", "hash", "Alice", "UTC");
        accessToken = jwtService.generateAccessToken("alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
    }

    @Test
    void reflect_validRequest_returns200() throws Exception {
        ReflectionRequest req = new ReflectionRequest((short) 4, (short) 3, "Good day", true, null);
        ReflectionResponse resp = new ReflectionResponse(
                UUID.randomUUID(), user.getId(), LocalDate.now(),
                ReflectionType.DAILY,
                (short) 4, (short) 3, "Good day", true,
                Instant.now(), Instant.now());
        when(reflectionService.upsert(any(AppUser.class), any(ReflectionRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/schedule/today/reflect")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.energyRating").value(4))
                .andExpect(jsonPath("$.isFinalized").value(true));
    }

    @Test
    void reflect_invalidRating_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/schedule/today/reflect")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"energyRating\":6,\"moodRating\":3,\"isFinalized\":false}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reflect_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/schedule/today/reflect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"energyRating\":3,\"moodRating\":3,\"isFinalized\":false}"))
                .andExpect(status().isUnauthorized());
    }
}
