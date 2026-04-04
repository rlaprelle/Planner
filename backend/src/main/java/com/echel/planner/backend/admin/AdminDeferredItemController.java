package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminDeferredItemRequest;
import com.echel.planner.backend.admin.dto.AdminDeferredItemResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/deferred-items")
public class AdminDeferredItemController {

    private final AdminDeferredItemService adminDeferredItemService;

    public AdminDeferredItemController(AdminDeferredItemService adminDeferredItemService) {
        this.adminDeferredItemService = adminDeferredItemService;
    }

    @GetMapping
    public ResponseEntity<List<AdminDeferredItemResponse>> listAll() {
        return ResponseEntity.ok(adminDeferredItemService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminDeferredItemResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(adminDeferredItemService.get(id));
    }

    @PostMapping
    public ResponseEntity<AdminDeferredItemResponse> create(@Valid @RequestBody AdminDeferredItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminDeferredItemService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminDeferredItemResponse> update(@PathVariable UUID id,
                                                             @Valid @RequestBody AdminDeferredItemRequest request) {
        return ResponseEntity.ok(adminDeferredItemService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        adminDeferredItemService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
