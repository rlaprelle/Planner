package com.planner.backend.reflection;

import com.planner.backend.auth.AppUser;
import com.planner.backend.reflection.dto.ReflectionRequest;
import com.planner.backend.reflection.dto.ReflectionResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DailyReflectionController {

    private final DailyReflectionService service;

    public DailyReflectionController(DailyReflectionService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/schedule/today/reflect")
    public ResponseEntity<ReflectionResponse> reflect(
            @AuthenticationPrincipal AppUser user,
            @Valid @RequestBody ReflectionRequest request) {
        return ResponseEntity.ok(service.upsert(user, request));
    }
}
