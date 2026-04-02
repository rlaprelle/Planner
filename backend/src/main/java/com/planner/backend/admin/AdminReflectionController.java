package com.planner.backend.admin;

import com.planner.backend.admin.dto.AdminReflectionRequest;
import com.planner.backend.admin.dto.AdminReflectionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/reflections")
public class AdminReflectionController {

    private final AdminReflectionService adminReflectionService;

    public AdminReflectionController(AdminReflectionService adminReflectionService) {
        this.adminReflectionService = adminReflectionService;
    }

    @GetMapping
    public ResponseEntity<List<AdminReflectionResponse>> listAll() {
        return ResponseEntity.ok(adminReflectionService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminReflectionResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(adminReflectionService.get(id));
    }

    @PostMapping
    public ResponseEntity<AdminReflectionResponse> create(@Valid @RequestBody AdminReflectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminReflectionService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminReflectionResponse> update(@PathVariable UUID id,
                                                           @Valid @RequestBody AdminReflectionRequest request) {
        return ResponseEntity.ok(adminReflectionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        adminReflectionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
