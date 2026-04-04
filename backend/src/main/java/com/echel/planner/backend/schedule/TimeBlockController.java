package com.echel.planner.backend.schedule;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.schedule.dto.ExtendRequest;
import com.echel.planner.backend.schedule.dto.TimeBlockResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/time-blocks")
public class TimeBlockController {

    private final ScheduleService scheduleService;

    public TimeBlockController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PatchMapping("/{id}/start")
    public ResponseEntity<TimeBlockResponse> start(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(scheduleService.startBlock(user, id));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<TimeBlockResponse> complete(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(scheduleService.completeBlock(user, id));
    }

    @PatchMapping("/{id}/done-for-now")
    public ResponseEntity<TimeBlockResponse> doneForNow(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id) {
        return ResponseEntity.ok(scheduleService.doneForNow(user, id));
    }

    @PostMapping("/{id}/extend")
    public ResponseEntity<TimeBlockResponse> extend(
            @AuthenticationPrincipal AppUser user,
            @PathVariable UUID id,
            @RequestBody @Valid ExtendRequest request) {
        return ResponseEntity.ok(scheduleService.extendBlock(user, id, request.durationMinutes()));
    }
}
