package com.planner.backend.stats;

import com.planner.backend.auth.AppUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/streak")
    public ResponseEntity<Map<String, Integer>> streak(@AuthenticationPrincipal AppUser user) {
        return ResponseEntity.ok(Map.of("streak", statsService.getStreak(user)));
    }
}
