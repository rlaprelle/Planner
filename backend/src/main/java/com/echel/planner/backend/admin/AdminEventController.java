package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminEventRequest;
import com.echel.planner.backend.admin.dto.AdminEventResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/events")
public class AdminEventController {

    private final AdminEventService adminEventService;

    public AdminEventController(AdminEventService adminEventService) {
        this.adminEventService = adminEventService;
    }

    @GetMapping
    public ResponseEntity<List<AdminEventResponse>> listAll() {
        return ResponseEntity.ok(adminEventService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminEventResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(adminEventService.get(id));
    }

    @PostMapping
    public ResponseEntity<AdminEventResponse> create(@Valid @RequestBody AdminEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminEventService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminEventResponse> update(@PathVariable UUID id,
                                                      @Valid @RequestBody AdminEventRequest request) {
        return ResponseEntity.ok(adminEventService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        adminEventService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
