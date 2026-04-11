package com.echel.planner.backend.deferred;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.deferred.dto.ConvertToEventRequest;
import com.echel.planner.backend.deferred.dto.ConvertToTaskRequest;
import com.echel.planner.backend.deferred.dto.DeferRequest;
import com.echel.planner.backend.deferred.dto.DeferredItemCreateRequest;
import com.echel.planner.backend.deferred.dto.DeferredItemResponse;
import com.echel.planner.backend.event.dto.EventResponse;
import com.echel.planner.backend.task.dto.TaskResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class DeferredItemController {

    private final DeferredItemService service;

    public DeferredItemController(DeferredItemService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/deferred")
    public ResponseEntity<DeferredItemResponse> create(
            @AuthenticationPrincipal AppUser user,
            @Valid @RequestBody DeferredItemCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(user, request));
    }

    @GetMapping("/api/v1/deferred")
    public ResponseEntity<List<DeferredItemResponse>> listPending(
            @AuthenticationPrincipal AppUser user) {
        return ResponseEntity.ok(service.listPending(user));
    }

    @PostMapping("/api/v1/deferred/{id}/convert")
    public ResponseEntity<TaskResponse> convert(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @Valid @RequestBody ConvertToTaskRequest request) {
        return ResponseEntity.ok(service.convert(user, id, request));
    }

    @PostMapping("/api/v1/deferred/{id}/convert-to-event")
    public ResponseEntity<EventResponse> convertToEvent(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @Valid @RequestBody ConvertToEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.convertToEvent(user, id, request));
    }

    @PostMapping("/api/v1/deferred/{id}/defer")
    public ResponseEntity<DeferredItemResponse> defer(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @Valid @RequestBody DeferRequest request) {
        return ResponseEntity.ok(service.defer(user, id, request));
    }

    @PatchMapping("/api/v1/deferred/{id}/dismiss")
    public ResponseEntity<DeferredItemResponse> dismiss(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(service.dismiss(user, id));
    }
}
