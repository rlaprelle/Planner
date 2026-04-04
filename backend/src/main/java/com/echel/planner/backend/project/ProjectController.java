package com.echel.planner.backend.project;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.project.dto.ProjectCreateRequest;
import com.echel.planner.backend.project.dto.ProjectResponse;
import com.echel.planner.backend.project.dto.ProjectUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(
            @AuthenticationPrincipal AppUser user,
            @Valid @RequestBody ProjectCreateRequest request) {
        ProjectResponse response = projectService.create(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> listActive(
            @AuthenticationPrincipal AppUser user) {
        return ResponseEntity.ok(projectService.listActive(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> get(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(projectService.get(user, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> update(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @Valid @RequestBody ProjectUpdateRequest request) {
        return ResponseEntity.ok(projectService.update(user, id, request));
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<ProjectResponse> archive(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(projectService.archive(user, id));
    }
}
