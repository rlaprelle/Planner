package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminProjectRequest;
import com.echel.planner.backend.admin.dto.AdminProjectResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/projects")
public class AdminProjectController {

    private final AdminProjectService adminProjectService;

    public AdminProjectController(AdminProjectService adminProjectService) {
        this.adminProjectService = adminProjectService;
    }

    @GetMapping
    public ResponseEntity<List<AdminProjectResponse>> listAll() {
        return ResponseEntity.ok(adminProjectService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminProjectResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(adminProjectService.get(id));
    }

    @PostMapping
    public ResponseEntity<AdminProjectResponse> create(@Valid @RequestBody AdminProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminProjectService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminProjectResponse> update(@PathVariable UUID id,
                                                        @Valid @RequestBody AdminProjectRequest request) {
        return ResponseEntity.ok(adminProjectService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        adminProjectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
