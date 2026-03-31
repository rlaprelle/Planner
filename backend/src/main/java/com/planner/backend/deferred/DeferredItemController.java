package com.planner.backend.deferred;

import com.planner.backend.auth.AppUser;
import com.planner.backend.deferred.dto.DeferredItemCreateRequest;
import com.planner.backend.deferred.dto.DeferredItemResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class DeferredItemController {

    private final DeferredItemService service;

    public DeferredItemController(DeferredItemService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/deferred")
    public ResponseEntity<DeferredItemResponse> create(
            @AuthenticationPrincipal AppUser user,
            @Valid @RequestBody DeferredItemCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(user, request));
    }

    @GetMapping("/api/v1/deferred")
    public ResponseEntity<List<DeferredItemResponse>> listPending(
            @AuthenticationPrincipal AppUser user) {
        return ResponseEntity.ok(service.listPending(user));
    }
}
