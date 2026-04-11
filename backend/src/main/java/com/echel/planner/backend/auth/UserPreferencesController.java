package com.echel.planner.backend.auth;

import com.echel.planner.backend.auth.dto.PreferencesResponse;
import com.echel.planner.backend.auth.dto.UpdatePreferencesRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user/preferences")
public class UserPreferencesController {

    private final UserPreferencesService preferencesService;

    public UserPreferencesController(UserPreferencesService preferencesService) {
        this.preferencesService = preferencesService;
    }

    @GetMapping
    public ResponseEntity<PreferencesResponse> getPreferences(
            @AuthenticationPrincipal AppUser user) {
        return ResponseEntity.ok(preferencesService.getPreferences(user));
    }

    @PatchMapping
    public ResponseEntity<PreferencesResponse> updatePreferences(
            @AuthenticationPrincipal AppUser user,
            @RequestBody UpdatePreferencesRequest request) {
        return ResponseEntity.ok(preferencesService.updatePreferences(user, request));
    }
}
