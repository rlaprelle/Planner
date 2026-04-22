package com.echel.planner.backend.admin;

import com.echel.planner.backend.admin.dto.AdminUserRequest;
import com.echel.planner.backend.admin.dto.AdminUserResponse;
import com.echel.planner.backend.admin.dto.DependentCountResponse;
import com.echel.planner.backend.auth.AppUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> listAll() {
        return ResponseEntity.ok(adminUserService.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminUserResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(adminUserService.get(id));
    }

    @PostMapping
    public ResponseEntity<AdminUserResponse> create(@Valid @RequestBody AdminUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminUserService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminUserResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUserRequest request,
            @AuthenticationPrincipal AppUser currentAdmin) {
        return ResponseEntity.ok(adminUserService.update(id, request, currentAdmin.getId()));
    }

    @GetMapping("/{id}/dependents")
    public ResponseEntity<DependentCountResponse> getDependents(@PathVariable UUID id) {
        return ResponseEntity.ok(adminUserService.getDependentCounts(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        adminUserService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
