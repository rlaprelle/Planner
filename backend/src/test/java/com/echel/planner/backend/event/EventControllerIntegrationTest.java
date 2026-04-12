package com.echel.planner.backend.event;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.common.EntityNotFoundException;
import com.echel.planner.backend.common.ValidationException;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.auth.JwtAuthFilter;
import com.echel.planner.backend.auth.JwtService;
import com.echel.planner.backend.auth.SecurityConfig;
import com.echel.planner.backend.event.dto.EventCreateRequest;
import com.echel.planner.backend.event.dto.EventResponse;
import com.echel.planner.backend.event.dto.EventUpdateRequest;
import com.echel.planner.backend.task.EnergyLevel;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthFilter.class})
class EventControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private EventService eventService;

    @MockBean
    private AppUserRepository appUserRepository;

    private AppUser testUser;
    private String accessToken;
    private UUID projectId;
    private UUID eventId;
    private EventResponse sampleEvent;

    @BeforeEach
    void setUp() {
        testUser = new AppUser("alice@example.com", "hashed", "Alice", "UTC");
        accessToken = jwtService.generateAccessToken("alice@example.com");

        projectId = UUID.randomUUID();
        eventId = UUID.randomUUID();

        Instant now = Instant.now();
        sampleEvent = new EventResponse(
                eventId,
                projectId,
                "My Project",
                "#6B7280",
                testUser.getId(),
                "Team standup",
                "Daily sync",
                EnergyLevel.MEDIUM,
                LocalDate.of(2026, 4, 5),
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                null,
                now,
                now
        );

        when(appUserRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(testUser));
    }

    // --- POST /api/v1/projects/{projectId}/events ---

    @Test
    void createEvent_authenticated_returns201WithEvent() throws Exception {
        EventCreateRequest req = new EventCreateRequest(
                "Team standup",
                "Daily sync",
                EnergyLevel.MEDIUM,
                LocalDate.of(2026, 4, 5),
                LocalTime.of(9, 0),
                LocalTime.of(9, 30)
        );

        when(eventService.create(any(AppUser.class), any(UUID.class), any(EventCreateRequest.class)))
                .thenReturn(sampleEvent);

        mockMvc.perform(post("/api/v1/projects/{projectId}/events", projectId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(eventId.toString()))
                .andExpect(jsonPath("$.title").value("Team standup"))
                .andExpect(jsonPath("$.projectId").value(projectId.toString()));
    }

    @Test
    void createEvent_missingTitle_returns400() throws Exception {
        EventCreateRequest req = new EventCreateRequest(
                "",  // blank title
                null,
                null,
                LocalDate.of(2026, 4, 5),
                LocalTime.of(9, 0),
                LocalTime.of(9, 30)
        );

        mockMvc.perform(post("/api/v1/projects/{projectId}/events", projectId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEvent_missingBlockDate_returns400() throws Exception {
        EventCreateRequest req = new EventCreateRequest(
                "Team standup",
                null,
                null,
                null,  // missing blockDate
                LocalTime.of(9, 0),
                LocalTime.of(9, 30)
        );

        mockMvc.perform(post("/api/v1/projects/{projectId}/events", projectId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/v1/projects/{projectId}/events ---

    @Test
    void listByProject_authenticated_returns200WithArray() throws Exception {
        when(eventService.listByProject(any(AppUser.class), eq(projectId)))
                .thenReturn(List.of(sampleEvent));

        mockMvc.perform(get("/api/v1/projects/{projectId}/events", projectId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(eventId.toString()))
                .andExpect(jsonPath("$[0].title").value("Team standup"));
    }

    // --- GET /api/v1/events/{id} ---

    @Test
    void getEvent_authenticated_returns200WithEvent() throws Exception {
        when(eventService.get(any(AppUser.class), eq(eventId)))
                .thenReturn(sampleEvent);

        mockMvc.perform(get("/api/v1/events/{id}", eventId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId.toString()))
                .andExpect(jsonPath("$.title").value("Team standup"))
                .andExpect(jsonPath("$.blockDate").value("2026-04-05"));
    }

    @Test
    void getEvent_notFound_returns404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(eventService.get(any(AppUser.class), eq(unknownId)))
                .thenThrow(new EntityNotFoundException("Event not found: " + unknownId));

        mockMvc.perform(get("/api/v1/events/{id}", unknownId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Event not found: " + unknownId));
    }

    // --- PUT /api/v1/events/{id} ---

    @Test
    void updateEvent_authenticated_returns200WithUpdatedEvent() throws Exception {
        EventUpdateRequest req = new EventUpdateRequest(
                null,
                "Updated standup",
                "New description",
                EnergyLevel.HIGH,
                null,
                null,
                null
        );

        EventResponse updated = new EventResponse(
                eventId,
                projectId,
                "My Project",
                "#6B7280",
                testUser.getId(),
                "Updated standup",
                "New description",
                EnergyLevel.HIGH,
                LocalDate.of(2026, 4, 5),
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                null,
                sampleEvent.createdAt(),
                Instant.now()
        );

        when(eventService.update(any(AppUser.class), eq(eventId), any(EventUpdateRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/v1/events/{id}", eventId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated standup"))
                .andExpect(jsonPath("$.description").value("New description"))
                .andExpect(jsonPath("$.energyLevel").value("HIGH"));
    }

    @Test
    void updateEvent_validationError_returns422() throws Exception {
        EventUpdateRequest req = new EventUpdateRequest(
                null, null, null, null,
                null,
                LocalTime.of(10, 0),
                LocalTime.of(9, 0)  // end before start
        );

        when(eventService.update(any(AppUser.class), eq(eventId), any(EventUpdateRequest.class)))
                .thenThrow(new ValidationException(
                        "End time must be after start time: 10:00 - 09:00"));

        mockMvc.perform(put("/api/v1/events/{id}", eventId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("End time must be after start time: 10:00 - 09:00"));
    }

    // --- PATCH /api/v1/events/{id}/archive ---

    @Test
    void archiveEvent_authenticated_returns200WithArchivedAt() throws Exception {
        Instant archivedAt = Instant.now();
        EventResponse archived = new EventResponse(
                eventId,
                projectId,
                "My Project",
                "#6B7280",
                testUser.getId(),
                "Team standup",
                "Daily sync",
                EnergyLevel.MEDIUM,
                LocalDate.of(2026, 4, 5),
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                archivedAt,
                sampleEvent.createdAt(),
                Instant.now()
        );

        when(eventService.archive(any(AppUser.class), eq(eventId)))
                .thenReturn(archived);

        mockMvc.perform(patch("/api/v1/events/{id}/archive", eventId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId.toString()))
                .andExpect(jsonPath("$.archivedAt").isNotEmpty());
    }

    // --- Unauthenticated requests ---

    @Test
    void anyEndpoint_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/events/{id}", eventId))
                .andExpect(status().isUnauthorized());
    }
}
