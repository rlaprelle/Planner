package com.echel.planner.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.echel.planner.backend.auth.dto.UpdatePreferencesRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserPreferencesController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthFilter.class,
         UserPreferencesService.class, com.echel.planner.backend.common.GlobalExceptionHandler.class})
class UserPreferencesControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockBean private AppUserRepository userRepository;

    private AppUser testUser;
    private String accessToken;

    @BeforeEach
    void setUp() {
        testUser = new AppUser("alice@example.com",
                passwordEncoder.encode("secret123"), "Alice", "America/New_York");
        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);
        accessToken = jwtService.generateAccessToken("alice@example.com");
    }

    @Test
    void getPreferences_authenticated_returnsDefaults() throws Exception {
        mockMvc.perform(get("/api/v1/user/preferences")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Alice"))
                .andExpect(jsonPath("$.timezone").value("America/New_York"))
                .andExpect(jsonPath("$.defaultSessionMinutes").value(60))
                .andExpect(jsonPath("$.weekStartDay").value("MONDAY"))
                .andExpect(jsonPath("$.ceremonyDay").value("FRIDAY"));
    }

    @Test
    void getPreferences_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/user/preferences"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updatePreferences_partialUpdate_returns200() throws Exception {
        var request = new UpdatePreferencesRequest(
                null, null, null, null, null, null, DayOfWeek.THURSDAY
        );

        mockMvc.perform(patch("/api/v1/user/preferences")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ceremonyDay").value("THURSDAY"))
                .andExpect(jsonPath("$.displayName").value("Alice"));
    }

    @Test
    void updatePreferences_invalidTimezone_returns422() throws Exception {
        var request = new UpdatePreferencesRequest(
                null, "Not/A/Zone", null, null, null, null, null
        );

        mockMvc.perform(patch("/api/v1/user/preferences")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void updatePreferences_invalidSessionMinutes_returns422() throws Exception {
        var request = new UpdatePreferencesRequest(
                null, null, null, null, 25, null, null
        );

        mockMvc.perform(patch("/api/v1/user/preferences")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }
}
