package com.planner.backend.admin;

import com.planner.backend.admin.dto.AdminTimeBlockRequest;
import com.planner.backend.admin.dto.AdminTimeBlockResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/time-blocks")
public class AdminTimeBlockController {

    private final AdminTimeBlockService adminTimeBlockService;

    public AdminTimeBlockController(AdminTimeBlockService adminTimeBlockService) {
        this.adminTimeBlockService = adminTimeBlockService;
    }

    @GetMapping
    public ResponseEntity<List<AdminTimeBlockResponse>> listAll() {
        return ResponseEntity.ok(adminTimeBlockService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminTimeBlockResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(adminTimeBlockService.get(id));
    }

    @PostMapping
    public ResponseEntity<AdminTimeBlockResponse> create(@Valid @RequestBody AdminTimeBlockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminTimeBlockService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminTimeBlockResponse> update(@PathVariable UUID id,
                                                          @Valid @RequestBody AdminTimeBlockRequest request) {
        return ResponseEntity.ok(adminTimeBlockService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        adminTimeBlockService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
