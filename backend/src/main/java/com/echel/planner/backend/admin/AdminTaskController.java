package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminTaskRequest;
import com.echel.planner.backend.admin.dto.AdminTaskResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/tasks")
public class AdminTaskController {

    private final AdminTaskService adminTaskService;

    public AdminTaskController(AdminTaskService adminTaskService) {
        this.adminTaskService = adminTaskService;
    }

    @GetMapping
    public ResponseEntity<List<AdminTaskResponse>> listAll() {
        return ResponseEntity.ok(adminTaskService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminTaskResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(adminTaskService.get(id));
    }

    @PostMapping
    public ResponseEntity<AdminTaskResponse> create(@Valid @RequestBody AdminTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminTaskService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminTaskResponse> update(@PathVariable UUID id,
                                                     @Valid @RequestBody AdminTaskRequest request) {
        return ResponseEntity.ok(adminTaskService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        adminTaskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
