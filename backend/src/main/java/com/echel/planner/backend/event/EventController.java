package com.echel.planner.backend.event;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.event.dto.EventCreateRequest;
import com.echel.planner.backend.event.dto.EventResponse;
import com.echel.planner.backend.event.dto.EventUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for event CRUD operations.
 * Events are time-blocked calendar entries belonging to a project.
 */
@RestController
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /** Creates a new event for the given project. */
    @PostMapping("/api/v1/projects/{projectId}/events")
    public ResponseEntity<EventResponse> create(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID projectId,
            @Valid @RequestBody EventCreateRequest request) {
        EventResponse response = eventService.create(user, projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Lists non-archived events for a project. */
    @GetMapping("/api/v1/projects/{projectId}/events")
    public ResponseEntity<List<EventResponse>> listByProject(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID projectId) {
        return ResponseEntity.ok(eventService.listByProject(user, projectId));
    }

    /** Returns a single event by id. */
    @GetMapping("/api/v1/events/{id}")
    public ResponseEntity<EventResponse> get(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(eventService.get(user, id));
    }

    /** Partially updates an event. Only non-null fields are applied. */
    @PutMapping("/api/v1/events/{id}")
    public ResponseEntity<EventResponse> update(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @Valid @RequestBody EventUpdateRequest request) {
        return ResponseEntity.ok(eventService.update(user, id, request));
    }

    /** Archives an event by setting its archived_at timestamp. */
    @PatchMapping("/api/v1/events/{id}/archive")
    public ResponseEntity<EventResponse> archive(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(eventService.archive(user, id));
    }
}
