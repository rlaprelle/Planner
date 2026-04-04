package com.echel.planner.backend.schedule;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.schedule.dto.SavePlanRequest;
import com.echel.planner.backend.schedule.dto.TimeBlockResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schedule/today")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public ResponseEntity<List<TimeBlockResponse>> getToday(@AuthenticationPrincipal AppUser user) {
        return ResponseEntity.ok(scheduleService.getToday(user));
    }

    @PostMapping("/plan")
    public ResponseEntity<List<TimeBlockResponse>> savePlan(
            @AuthenticationPrincipal AppUser user,
            @Valid @RequestBody SavePlanRequest request) {
        return ResponseEntity.ok(scheduleService.savePlan(user, request));
    }
}
