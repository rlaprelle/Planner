package com.echel.planner.backend.task;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.task.dto.TaskCreateRequest;
import com.echel.planner.backend.task.dto.TaskResponse;
import com.echel.planner.backend.task.dto.TaskStatusRequest;
import com.echel.planner.backend.task.dto.TaskUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/api/v1/projects/{projectId}/tasks")
    public ResponseEntity<TaskResponse> create(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID projectId,
            @Valid @RequestBody TaskCreateRequest request) {
        TaskResponse response = taskService.create(user, projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/v1/projects/{projectId}/tasks")
    public ResponseEntity<List<TaskResponse>> listTopLevel(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID projectId) {
        return ResponseEntity.ok(taskService.listTopLevel(user, projectId));
    }

    @GetMapping("/api/v1/tasks/{id}")
    public ResponseEntity<TaskResponse> get(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(taskService.get(user, id));
    }

    @PutMapping("/api/v1/tasks/{id}")
    public ResponseEntity<TaskResponse> update(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @Valid @RequestBody TaskUpdateRequest request) {
        return ResponseEntity.ok(taskService.update(user, id, request));
    }

    @PatchMapping("/api/v1/tasks/{id}/archive")
    public ResponseEntity<TaskResponse> archive(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(taskService.archive(user, id));
    }

    @PatchMapping("/api/v1/tasks/{id}/status")
    public ResponseEntity<TaskResponse> changeStatus(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @Valid @RequestBody TaskStatusRequest request) {
        return ResponseEntity.ok(taskService.changeStatus(user, id, request));
    }

    @GetMapping("/api/v1/tasks/completed-today")
    public ResponseEntity<List<TaskResponse>> listCompletedToday(
            @AuthenticationPrincipal AppUser user) {
        return ResponseEntity.ok(taskService.listCompletedToday(user));
    }

    @GetMapping("/api/v1/tasks/suggested")
    public ResponseEntity<List<TaskResponse>> listSuggested(
            @AuthenticationPrincipal AppUser user,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "50") int limit) {
        LocalDate targetDate = date != null ? date
                : LocalDate.now(java.time.ZoneId.of(user.getTimezone()));
        return ResponseEntity.ok(taskService.listSuggested(user, targetDate, limit));
    }
}
