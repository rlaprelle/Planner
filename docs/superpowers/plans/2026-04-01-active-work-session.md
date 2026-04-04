# Active Work Session Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a full-screen focus mode where users start a time block, see a countdown timer with optional subtask checklist, and end the session by completing, extending, or stepping away.

**Architecture:** New `TimeBlockController` at `/api/v1/time-blocks` handles session lifecycle (start/complete/done-for-now/extend). Frontend adds an `ActiveSessionPage` at `/session/:blockId` with centered minimal layout, plus an `ActiveSessionContext` for header timer persistence when navigating away.

**Tech Stack:** Spring Boot (PATCH/POST endpoints), Flyway (migration), React, TanStack Query (mutations), SVG (timer circle), Web Audio API (chime), React Context (session state).

**Design spec:** `docs/superpowers/specs/2026-04-01-active-work-session-design.md`

---

## File Structure

### Backend — Create

| File | Responsibility |
|------|---------------|
| `backend/src/main/resources/db/migration/V7__add_time_block_session_fields.sql` | Add actual_start, actual_end, was_completed columns |
| `backend/src/main/java/com/echel/planner/backend/schedule/TimeBlockController.java` | REST endpoints for time block session lifecycle |
| `backend/src/main/java/com/echel/planner/backend/schedule/dto/ExtendRequest.java` | Request DTO for extend endpoint |
| `backend/src/test/java/com/echel/planner/backend/schedule/TimeBlockControllerIntegrationTest.java` | Integration tests for all 4 new endpoints |

### Backend — Modify

| File | Change |
|------|--------|
| `backend/src/main/java/com/echel/planner/backend/schedule/TimeBlock.java` | Add actualStart, actualEnd, wasCompleted fields + setters |
| `backend/src/main/java/com/echel/planner/backend/schedule/dto/TimeBlockResponse.java` | Add actualStart, actualEnd, wasCompleted to response record |
| `backend/src/main/java/com/echel/planner/backend/schedule/TimeBlockRepository.java` | Add findByIdAndUserId query |
| `backend/src/main/java/com/echel/planner/backend/schedule/ScheduleService.java` | Add startBlock, completeBlock, doneForNow, extendBlock methods |
| `backend/src/main/java/com/echel/planner/backend/schedule/ScheduleExceptionHandler.java` | Handle new exception types (block not found, already started) |

### Frontend — Create

| File | Responsibility |
|------|---------------|
| `frontend/src/contexts/ActiveSessionContext.jsx` | React context holding active block ID, task name, end time |
| `frontend/src/pages/ActiveSessionPage.jsx` | Full-screen focus view assembling timer + subtasks + actions |
| `frontend/src/pages/active-session/TimerCircle.jsx` | SVG circular countdown timer component |
| `frontend/src/pages/active-session/SubtaskChecklist.jsx` | Interactive child task checklist |
| `frontend/src/pages/active-session/chime.js` | Web Audio API completion chime |

### Frontend — Modify

| File | Change |
|------|--------|
| `frontend/src/api/schedule.js` | Add startTimeBlock, completeTimeBlock, doneForNowTimeBlock, extendTimeBlock |
| `frontend/src/App.jsx` | Add `/session/:blockId` route, wrap with ActiveSessionProvider |
| `frontend/src/layouts/AppLayout.jsx` | Add compact header timer widget when session active |
| `frontend/src/pages/start-day/TimeBlock.jsx` | Add "Start" button to each block |
| `frontend/src/pages/DashboardPage.jsx` | Add "Start" button for next upcoming block |

### E2E — Create/Modify

| File | Change |
|------|--------|
| `e2e/tests/active-session.spec.ts` | E2E tests for session flow |
| `e2e/fixtures/data.ts` | Add time block mock data with session fields |
| `e2e/fixtures/mocks.ts` | Add mock helpers for session endpoints |

---

## Task 1: Database Migration

**Files:**
- Create: `backend/src/main/resources/db/migration/V7__add_time_block_session_fields.sql`

- [ ] **Step 1: Write the migration**

```sql
ALTER TABLE time_block ADD COLUMN actual_start TIMESTAMP WITH TIME ZONE;
ALTER TABLE time_block ADD COLUMN actual_end   TIMESTAMP WITH TIME ZONE;
ALTER TABLE time_block ADD COLUMN was_completed BOOLEAN NOT NULL DEFAULT false;
```

- [ ] **Step 2: Verify migration applies**

Run: `cd backend && mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/planner -Dflyway.user=planner -Dflyway.password=planner`

If Maven flyway plugin isn't configured, just run the app and check logs:
```bash
cd backend && mvn spring-boot:run
```
Expected: Flyway log line `Successfully applied 1 migration to schema "public", now at version v7`

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/resources/db/migration/V7__add_time_block_session_fields.sql
git commit -m "feat: add session fields to time_block table (V7 migration)"
```

---

## Task 2: Entity & DTO Updates

**Files:**
- Modify: `backend/src/main/java/com/echel/planner/backend/schedule/TimeBlock.java`
- Modify: `backend/src/main/java/com/echel/planner/backend/schedule/dto/TimeBlockResponse.java`
- Modify: `backend/src/main/java/com/echel/planner/backend/schedule/TimeBlockRepository.java`

- [ ] **Step 1: Add fields to TimeBlock entity**

Add these fields after the existing `sortOrder` field in `TimeBlock.java`:

```java
@Column(name = "actual_start")
private Instant actualStart;

@Column(name = "actual_end")
private Instant actualEnd;

@Column(name = "was_completed", nullable = false)
private boolean wasCompleted = false;
```

Add getters and setters for all three fields:

```java
public Instant getActualStart() { return actualStart; }
public void setActualStart(Instant actualStart) { this.actualStart = actualStart; }

public Instant getActualEnd() { return actualEnd; }
public void setActualEnd(Instant actualEnd) { this.actualEnd = actualEnd; }

public boolean isWasCompleted() { return wasCompleted; }
public void setWasCompleted(boolean wasCompleted) { this.wasCompleted = wasCompleted; }
```

Add `import java.time.Instant;` at the top if not already present.

- [ ] **Step 2: Add fields to TimeBlockResponse**

The existing `TimeBlockResponse` is a record. Add three new fields after `sortOrder`:

```java
public record TimeBlockResponse(
    UUID id,
    LocalDate blockDate,
    LocalTime startTime,
    LocalTime endTime,
    int sortOrder,
    Instant actualStart,
    Instant actualEnd,
    boolean wasCompleted,
    TaskSummary task
) {
```

Update the `from(TimeBlock)` factory method to pass the new fields:

```java
public static TimeBlockResponse from(TimeBlock tb) {
    TaskSummary ts = null;
    if (tb.getTask() != null) {
        var t = tb.getTask();
        ts = new TaskSummary(
            t.getId(), t.getTitle(),
            t.getProject().getId(), t.getProject().getName(),
            t.getProject().getColor(), t.getStatus().name(),
            t.getPointsEstimate()
        );
    }
    return new TimeBlockResponse(
        tb.getId(), tb.getBlockDate(),
        tb.getStartTime(), tb.getEndTime(), tb.getSortOrder(),
        tb.getActualStart(), tb.getActualEnd(), tb.isWasCompleted(),
        ts
    );
}
```

- [ ] **Step 3: Add findByIdAndUserId to TimeBlockRepository**

Add this method to `TimeBlockRepository.java`:

```java
@Query("SELECT tb FROM TimeBlock tb LEFT JOIN FETCH tb.task t LEFT JOIN FETCH t.project WHERE tb.id = :id AND tb.user.id = :userId")
Optional<TimeBlock> findByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
```

Add `import java.util.Optional;` if not already present.

- [ ] **Step 4: Verify compilation**

Run: `cd backend && mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/echel/planner/backend/schedule/TimeBlock.java \
       backend/src/main/java/com/echel/planner/backend/schedule/dto/TimeBlockResponse.java \
       backend/src/main/java/com/echel/planner/backend/schedule/TimeBlockRepository.java
git commit -m "feat: add session fields to TimeBlock entity, response, and repository"
```

---

## Task 3: Start Endpoint

**Files:**
- Create: `backend/src/test/java/com/echel/planner/backend/schedule/TimeBlockControllerIntegrationTest.java`
- Create: `backend/src/main/java/com/echel/planner/backend/schedule/TimeBlockController.java`
- Modify: `backend/src/main/java/com/echel/planner/backend/schedule/ScheduleService.java`

- [ ] **Step 1: Write the failing test**

Create `TimeBlockControllerIntegrationTest.java`:

```java
package com.echel.planner.backend.schedule;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.auth.JwtAuthFilter;
import com.echel.planner.backend.auth.JwtService;
import com.echel.planner.backend.auth.SecurityConfig;
import com.echel.planner.backend.schedule.dto.TimeBlockResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TimeBlockController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthFilter.class, ScheduleExceptionHandler.class})
class TimeBlockControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtService jwtService;

    @MockBean AppUserRepository userRepository;
    @MockBean ScheduleService scheduleService;

    private AppUser user;
    private String token;
    private UUID blockId;

    @BeforeEach
    void setUp() {
        user = new AppUser("alice@example.com", "hash", "Alice", "UTC");
        token = jwtService.generateAccessToken("alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        blockId = UUID.randomUUID();
    }

    @Test
    void start_returnsUpdatedBlock() throws Exception {
        var response = new TimeBlockResponse(
            blockId, LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0), 0,
            Instant.now(), null, false,
            new TimeBlockResponse.TaskSummary(UUID.randomUUID(), "Write tests",
                UUID.randomUUID(), "Work", "#6366f1", "IN_PROGRESS", 2)
        );
        when(scheduleService.startBlock(any(AppUser.class), any(UUID.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/time-blocks/{id}/start", blockId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.actualStart").isNotEmpty())
            .andExpect(jsonPath("$.task.title").value("Write tests"));
    }

    @Test
    void start_noAuth_returns401() throws Exception {
        mockMvc.perform(patch("/api/v1/time-blocks/{id}/start", blockId))
            .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl . -Dtest=TimeBlockControllerIntegrationTest -Dsurefire.failIfNoTests=false`
Expected: FAIL — `TimeBlockController` class not found

- [ ] **Step 3: Create TimeBlockController with start endpoint**

Create `TimeBlockController.java`:

```java
package com.echel.planner.backend.schedule;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.schedule.dto.TimeBlockResponse;
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
}
```

- [ ] **Step 4: Add startBlock to ScheduleService**

Add this method to `ScheduleService.java`:

```java
public TimeBlockResponse startBlock(AppUser user, UUID blockId) {
    TimeBlock block = timeBlockRepository.findByIdAndUserId(blockId, user.getId())
        .orElseThrow(() -> new ScheduleValidationException("Time block not found"));
    if (block.getActualStart() != null) {
        throw new ScheduleValidationException("Time block already started");
    }
    block.setActualStart(Instant.now());
    timeBlockRepository.save(block);
    return TimeBlockResponse.from(block);
}
```

Add `import java.time.Instant;` if not already present.

- [ ] **Step 5: Update ScheduleExceptionHandler for 409**

The existing handler maps `ScheduleValidationException` to 422. The "already started" case should also return 422 (it's a validation error), which works with the existing handler. No changes needed.

- [ ] **Step 6: Run tests to verify they pass**

Run: `cd backend && mvn test -pl . -Dtest=TimeBlockControllerIntegrationTest`
Expected: 2 tests PASS

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/echel/planner/backend/schedule/TimeBlockController.java \
       backend/src/main/java/com/echel/planner/backend/schedule/ScheduleService.java \
       backend/src/test/java/com/echel/planner/backend/schedule/TimeBlockControllerIntegrationTest.java
git commit -m "feat: add PATCH /time-blocks/{id}/start endpoint"
```

---

## Task 4: Complete Endpoint

**Files:**
- Modify: `backend/src/test/java/com/echel/planner/backend/schedule/TimeBlockControllerIntegrationTest.java`
- Modify: `backend/src/main/java/com/echel/planner/backend/schedule/TimeBlockController.java`
- Modify: `backend/src/main/java/com/echel/planner/backend/schedule/ScheduleService.java`

- [ ] **Step 1: Write the failing test**

Add to `TimeBlockControllerIntegrationTest.java`:

```java
@Test
void complete_returnsUpdatedBlock() throws Exception {
    var response = new TimeBlockResponse(
        blockId, LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0), 0,
        Instant.now().minusSeconds(3600), Instant.now(), true,
        new TimeBlockResponse.TaskSummary(UUID.randomUUID(), "Write tests",
            UUID.randomUUID(), "Work", "#6366f1", "DONE", 2)
    );
    when(scheduleService.completeBlock(any(AppUser.class), any(UUID.class))).thenReturn(response);

    mockMvc.perform(patch("/api/v1/time-blocks/{id}/complete", blockId)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.wasCompleted").value(true))
        .andExpect(jsonPath("$.actualEnd").isNotEmpty())
        .andExpect(jsonPath("$.task.status").value("DONE"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl . -Dtest=TimeBlockControllerIntegrationTest#complete_returnsUpdatedBlock`
Expected: FAIL — no `completeBlock` method

- [ ] **Step 3: Add complete endpoint to controller**

Add to `TimeBlockController.java`:

```java
@PatchMapping("/{id}/complete")
public ResponseEntity<TimeBlockResponse> complete(
        @AuthenticationPrincipal AppUser user,
        @PathVariable UUID id) {
    return ResponseEntity.ok(scheduleService.completeBlock(user, id));
}
```

- [ ] **Step 4: Add completeBlock to ScheduleService**

Add to `ScheduleService.java`. This needs a `TaskRepository` injected — add it to the constructor if not already present.

```java
public TimeBlockResponse completeBlock(AppUser user, UUID blockId) {
    TimeBlock block = timeBlockRepository.findByIdAndUserId(blockId, user.getId())
        .orElseThrow(() -> new ScheduleValidationException("Time block not found"));
    if (block.getActualStart() == null) {
        throw new ScheduleValidationException("Time block has not been started");
    }

    Instant now = Instant.now();
    block.setActualEnd(now);
    block.setWasCompleted(true);
    timeBlockRepository.save(block);

    if (block.getTask() != null) {
        long elapsedMinutes = java.time.Duration.between(block.getActualStart(), now).toMinutes();
        var task = block.getTask();
        int current = task.getActualMinutes() != null ? task.getActualMinutes() : 0;
        task.setActualMinutes(current + (int) elapsedMinutes);
        task.setStatus(com.echel.planner.backend.task.TaskStatus.DONE);
        task.setCompletedAt(now);
        taskRepository.save(task);
    }

    return TimeBlockResponse.from(block);
}
```

Make sure `TaskRepository` is injected. If the constructor currently only takes `TimeBlockRepository`, update it:

```java
private final TimeBlockRepository timeBlockRepository;
private final TaskRepository taskRepository;

public ScheduleService(TimeBlockRepository timeBlockRepository, TaskRepository taskRepository) {
    this.timeBlockRepository = timeBlockRepository;
    this.taskRepository = taskRepository;
}
```

Add `import com.echel.planner.backend.task.TaskRepository;` and `import com.echel.planner.backend.task.TaskStatus;` at the top.

Note: If `TaskRepository` is already injected (check the existing constructor), skip the constructor change.

- [ ] **Step 5: Run tests to verify they pass**

Run: `cd backend && mvn test -pl . -Dtest=TimeBlockControllerIntegrationTest`
Expected: 3 tests PASS

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/echel/planner/backend/schedule/TimeBlockController.java \
       backend/src/main/java/com/echel/planner/backend/schedule/ScheduleService.java \
       backend/src/test/java/com/echel/planner/backend/schedule/TimeBlockControllerIntegrationTest.java
git commit -m "feat: add PATCH /time-blocks/{id}/complete endpoint"
```

---

## Task 5: Done-for-now Endpoint

**Files:**
- Modify: `backend/src/test/java/com/echel/planner/backend/schedule/TimeBlockControllerIntegrationTest.java`
- Modify: `backend/src/main/java/com/echel/planner/backend/schedule/TimeBlockController.java`
- Modify: `backend/src/main/java/com/echel/planner/backend/schedule/ScheduleService.java`

- [ ] **Step 1: Write the failing test**

Add to `TimeBlockControllerIntegrationTest.java`:

```java
@Test
void doneForNow_returnsUpdatedBlock() throws Exception {
    var response = new TimeBlockResponse(
        blockId, LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0), 0,
        Instant.now().minusSeconds(1800), Instant.now(), false,
        new TimeBlockResponse.TaskSummary(UUID.randomUUID(), "Write tests",
            UUID.randomUUID(), "Work", "#6366f1", "IN_PROGRESS", 2)
    );
    when(scheduleService.doneForNow(any(AppUser.class), any(UUID.class))).thenReturn(response);

    mockMvc.perform(patch("/api/v1/time-blocks/{id}/done-for-now", blockId)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.wasCompleted").value(false))
        .andExpect(jsonPath("$.actualEnd").isNotEmpty())
        .andExpect(jsonPath("$.task.status").value("IN_PROGRESS"));
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn test -pl . -Dtest=TimeBlockControllerIntegrationTest#doneForNow_returnsUpdatedBlock`
Expected: FAIL

- [ ] **Step 3: Add done-for-now endpoint to controller**

Add to `TimeBlockController.java`:

```java
@PatchMapping("/{id}/done-for-now")
public ResponseEntity<TimeBlockResponse> doneForNow(
        @AuthenticationPrincipal AppUser user,
        @PathVariable UUID id) {
    return ResponseEntity.ok(scheduleService.doneForNow(user, id));
}
```

- [ ] **Step 4: Add doneForNow to ScheduleService**

Add to `ScheduleService.java`:

```java
public TimeBlockResponse doneForNow(AppUser user, UUID blockId) {
    TimeBlock block = timeBlockRepository.findByIdAndUserId(blockId, user.getId())
        .orElseThrow(() -> new ScheduleValidationException("Time block not found"));
    if (block.getActualStart() == null) {
        throw new ScheduleValidationException("Time block has not been started");
    }

    Instant now = Instant.now();
    block.setActualEnd(now);
    block.setWasCompleted(false);
    timeBlockRepository.save(block);

    if (block.getTask() != null) {
        long elapsedMinutes = java.time.Duration.between(block.getActualStart(), now).toMinutes();
        var task = block.getTask();
        int current = task.getActualMinutes() != null ? task.getActualMinutes() : 0;
        task.setActualMinutes(current + (int) elapsedMinutes);
        taskRepository.save(task);
    }

    return TimeBlockResponse.from(block);
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `cd backend && mvn test -pl . -Dtest=TimeBlockControllerIntegrationTest`
Expected: 4 tests PASS

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/echel/planner/backend/schedule/TimeBlockController.java \
       backend/src/main/java/com/echel/planner/backend/schedule/ScheduleService.java \
       backend/src/test/java/com/echel/planner/backend/schedule/TimeBlockControllerIntegrationTest.java
git commit -m "feat: add PATCH /time-blocks/{id}/done-for-now endpoint"
```

---

## Task 6: Extend Endpoint

**Files:**
- Create: `backend/src/main/java/com/echel/planner/backend/schedule/dto/ExtendRequest.java`
- Modify: `backend/src/test/java/com/echel/planner/backend/schedule/TimeBlockControllerIntegrationTest.java`
- Modify: `backend/src/main/java/com/echel/planner/backend/schedule/TimeBlockController.java`
- Modify: `backend/src/main/java/com/echel/planner/backend/schedule/ScheduleService.java`

- [ ] **Step 1: Create ExtendRequest DTO**

```java
package com.echel.planner.backend.schedule.dto;

import jakarta.validation.constraints.NotNull;

public record ExtendRequest(@NotNull Integer durationMinutes) {}
```

- [ ] **Step 2: Write the failing test**

Add imports to `TimeBlockControllerIntegrationTest.java`:

```java
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.http.MediaType;
import com.echel.planner.backend.schedule.dto.ExtendRequest;
```

Add test:

```java
@Test
void extend_createsNewBlock() throws Exception {
    UUID newBlockId = UUID.randomUUID();
    var response = new TimeBlockResponse(
        newBlockId, LocalDate.now(), LocalTime.of(10, 0), LocalTime.of(10, 30), 1,
        null, null, false,
        new TimeBlockResponse.TaskSummary(UUID.randomUUID(), "Write tests",
            UUID.randomUUID(), "Work", "#6366f1", "IN_PROGRESS", 2)
    );
    when(scheduleService.extendBlock(any(AppUser.class), any(UUID.class), any(Integer.class)))
        .thenReturn(response);

    mockMvc.perform(post("/api/v1/time-blocks/{id}/extend", blockId)
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new ExtendRequest(30))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.startTime").value("10:00:00"))
        .andExpect(jsonPath("$.endTime").value("10:30:00"));
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `cd backend && mvn test -pl . -Dtest=TimeBlockControllerIntegrationTest#extend_createsNewBlock`
Expected: FAIL

- [ ] **Step 4: Add extend endpoint to controller**

Add to `TimeBlockController.java`:

```java
import com.echel.planner.backend.schedule.dto.ExtendRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
```

```java
@PostMapping("/{id}/extend")
public ResponseEntity<TimeBlockResponse> extend(
        @AuthenticationPrincipal AppUser user,
        @PathVariable UUID id,
        @RequestBody @Valid ExtendRequest request) {
    return ResponseEntity.ok(scheduleService.extendBlock(user, id, request.durationMinutes()));
}
```

- [ ] **Step 5: Add extendBlock to ScheduleService**

Add to `ScheduleService.java`:

```java
public TimeBlockResponse extendBlock(AppUser user, UUID blockId, int durationMinutes) {
    TimeBlock block = timeBlockRepository.findByIdAndUserId(blockId, user.getId())
        .orElseThrow(() -> new ScheduleValidationException("Time block not found"));

    LocalTime newStart = block.getEndTime();
    LocalTime newEnd = newStart.plusMinutes(durationMinutes);

    TimeBlock extension = new TimeBlock(
        block.getUser(), block.getBlockDate(), block.getTask(),
        newStart, newEnd, block.getSortOrder() + 1
    );
    timeBlockRepository.save(extension);
    return TimeBlockResponse.from(extension);
}
```

- [ ] **Step 6: Run tests to verify they pass**

Run: `cd backend && mvn test -pl . -Dtest=TimeBlockControllerIntegrationTest`
Expected: 5 tests PASS

- [ ] **Step 7: Run all backend tests**

Run: `cd backend && mvn test`
Expected: All tests PASS (existing + new)

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/echel/planner/backend/schedule/TimeBlockController.java \
       backend/src/main/java/com/echel/planner/backend/schedule/ScheduleService.java \
       backend/src/main/java/com/echel/planner/backend/schedule/dto/ExtendRequest.java \
       backend/src/test/java/com/echel/planner/backend/schedule/TimeBlockControllerIntegrationTest.java
git commit -m "feat: add POST /time-blocks/{id}/extend endpoint"
```

---

## Task 7: Frontend API Layer

**Files:**
- Modify: `frontend/src/api/schedule.js`

- [ ] **Step 1: Add session API functions**

Add these functions to `frontend/src/api/schedule.js`:

```javascript
export async function startTimeBlock(blockId) {
  const res = await authFetch(`${BASE}/time-blocks/${blockId}/start`, {
    method: 'PATCH',
  })
  return handleResponse(res)
}

export async function completeTimeBlock(blockId) {
  const res = await authFetch(`${BASE}/time-blocks/${blockId}/complete`, {
    method: 'PATCH',
  })
  return handleResponse(res)
}

export async function doneForNowTimeBlock(blockId) {
  const res = await authFetch(`${BASE}/time-blocks/${blockId}/done-for-now`, {
    method: 'PATCH',
  })
  return handleResponse(res)
}

export async function extendTimeBlock(blockId, durationMinutes) {
  const res = await authFetch(`${BASE}/time-blocks/${blockId}/extend`, {
    method: 'POST',
    body: JSON.stringify({ durationMinutes }),
  })
  return handleResponse(res)
}
```

Note: `BASE` should be `/api/v1` — check the existing file's pattern. If the existing functions use a different base path variable name, match it.

- [ ] **Step 2: Verify lint passes**

Run: `cd frontend && npm run lint`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add frontend/src/api/schedule.js
git commit -m "feat: add time block session API functions"
```

---

## Task 8: Completion Chime

**Files:**
- Create: `frontend/src/pages/active-session/chime.js`

- [ ] **Step 1: Create chime utility**

Reference the existing chime in `frontend/src/components/QuickCapture.jsx` (lines 6-42) for the pattern. This version is warmer and longer — lower fundamental, longer sustain:

```javascript
export function playCompletionChime() {
  try {
    const ctx = new AudioContext()
    const t = ctx.currentTime
    const duration = 4.0

    // Warm fundamental tone ~220 Hz (A3)
    const osc1 = ctx.createOscillator()
    const gain1 = ctx.createGain()
    osc1.type = 'sine'
    osc1.frequency.setValueAtTime(220, t)
    gain1.gain.setValueAtTime(0, t)
    gain1.gain.linearRampToValueAtTime(0.3, t + 0.02)
    gain1.gain.exponentialRampToValueAtTime(0.001, t + duration)
    osc1.connect(gain1)
    gain1.connect(ctx.destination)

    // Upper partial ~550 Hz for warmth
    const osc2 = ctx.createOscillator()
    const gain2 = ctx.createGain()
    osc2.type = 'sine'
    osc2.frequency.setValueAtTime(550, t)
    gain2.gain.setValueAtTime(0, t)
    gain2.gain.linearRampToValueAtTime(0.15, t + 0.02)
    gain2.gain.exponentialRampToValueAtTime(0.001, t + duration * 0.6)
    osc2.connect(gain2)
    gain2.connect(ctx.destination)

    osc1.start(t)
    osc1.stop(t + duration)
    osc2.start(t)
    osc2.stop(t + duration * 0.6)

    osc1.onended = () => ctx.close()
  } catch {
    // AudioContext unavailable — silently ignore
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/pages/active-session/chime.js
git commit -m "feat: add completion chime for active work session"
```

---

## Task 9: ActiveSessionContext

**Files:**
- Create: `frontend/src/contexts/ActiveSessionContext.jsx`

- [ ] **Step 1: Create the context**

```jsx
import { createContext, useContext, useState, useCallback } from 'react'

const ActiveSessionContext = createContext(null)

export function ActiveSessionProvider({ children }) {
  const [session, setSession] = useState(null)

  const startSession = useCallback((blockId, taskName, endTime) => {
    setSession({ blockId, taskName, endTime })
  }, [])

  const clearSession = useCallback(() => {
    setSession(null)
  }, [])

  return (
    <ActiveSessionContext.Provider value={{ session, startSession, clearSession }}>
      {children}
    </ActiveSessionContext.Provider>
  )
}

export function useActiveSession() {
  const ctx = useContext(ActiveSessionContext)
  if (!ctx) throw new Error('useActiveSession must be used within ActiveSessionProvider')
  return ctx
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/contexts/ActiveSessionContext.jsx
git commit -m "feat: add ActiveSessionContext for timer persistence"
```

---

## Task 10: Timer Circle Component

**Files:**
- Create: `frontend/src/pages/active-session/TimerCircle.jsx`

- [ ] **Step 1: Create the SVG timer component**

```jsx
import { useState, useEffect } from 'react'

const RADIUS = 70
const CIRCUMFERENCE = 2 * Math.PI * RADIUS

export default function TimerCircle({ endTime, totalMinutes, onTimeUp }) {
  const [remainingMs, setRemainingMs] = useState(() => endTime - Date.now())
  const [hasChimed, setHasChimed] = useState(false)

  useEffect(() => {
    const interval = setInterval(() => {
      const ms = endTime - Date.now()
      setRemainingMs(ms)
      if (ms <= 0 && !hasChimed) {
        setHasChimed(true)
        onTimeUp?.()
      }
    }, 1000)
    return () => clearInterval(interval)
  }, [endTime, hasChimed, onTimeUp])

  const totalMs = totalMinutes * 60 * 1000
  const elapsed = totalMs - remainingMs
  const progress = Math.min(elapsed / totalMs, 1)
  const offset = CIRCUMFERENCE * (1 - progress)
  const isOvertime = remainingMs < 0

  const absMs = Math.abs(remainingMs)
  const minutes = Math.floor(absMs / 60000)
  const seconds = Math.floor((absMs % 60000) / 1000)
  const timeStr = `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`

  return (
    <div className="flex flex-col items-center">
      <div className="relative">
        <svg width="180" height="180" className="transform -rotate-90">
          {/* Background circle */}
          <circle
            cx="90" cy="90" r={RADIUS}
            fill="none" stroke="#e8e4f0" strokeWidth="8"
          />
          {/* Progress arc */}
          <circle
            cx="90" cy="90" r={RADIUS}
            fill="none" stroke="#8b7ec8" strokeWidth="8"
            strokeDasharray={CIRCUMFERENCE}
            strokeDashoffset={offset}
            strokeLinecap="round"
            className={`transition-all duration-1000 ${isOvertime ? 'animate-pulse' : ''}`}
          />
        </svg>
        {/* Timer text centered */}
        <div className="absolute inset-0 flex flex-col items-center justify-center">
          {isOvertime ? (
            <>
              <span className="text-3xl font-light text-gray-400">+{timeStr}</span>
              <span className="text-sm text-indigo-400 mt-1 font-medium">
                Time's up. Good work!
              </span>
            </>
          ) : (
            <span className="text-4xl font-light text-gray-800 tracking-wide">{timeStr}</span>
          )}
        </div>
      </div>
      <span className="text-sm text-gray-400 mt-3">of {totalMinutes} min</span>
    </div>
  )
}
```

- [ ] **Step 2: Verify lint passes**

Run: `cd frontend && npm run lint`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/active-session/TimerCircle.jsx
git commit -m "feat: add TimerCircle SVG countdown component"
```

---

## Task 11: Subtask Checklist Component

**Files:**
- Create: `frontend/src/pages/active-session/SubtaskChecklist.jsx`

- [ ] **Step 1: Create the checklist component**

```jsx
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { updateTaskStatus } from '@/api/tasks'

export default function SubtaskChecklist({ subtasks, projectId }) {
  const queryClient = useQueryClient()

  const toggleMutation = useMutation({
    mutationFn: ({ taskId, isDone }) =>
      updateTaskStatus(taskId, isDone ? 'DONE' : 'TODO'),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tasks', projectId] })
      queryClient.invalidateQueries({ queryKey: ['schedule'] })
    },
  })

  if (!subtasks || subtasks.length === 0) return null

  const sorted = [...subtasks].sort((a, b) => {
    if (a.status === 'DONE' && b.status !== 'DONE') return 1
    if (a.status !== 'DONE' && b.status === 'DONE') return -1
    return (a.sortOrder ?? 0) - (b.sortOrder ?? 0)
  })

  return (
    <div className="bg-white rounded-2xl p-4 w-full max-w-sm">
      <div className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">
        Subtasks
      </div>
      <div className="space-y-1">
        {sorted.map((child) => {
          const isDone = child.status === 'DONE'
          return (
            <label
              key={child.id}
              className="flex items-center gap-3 py-2 px-1 rounded-lg hover:bg-gray-50 cursor-pointer transition-colors"
            >
              <input
                type="checkbox"
                checked={isDone}
                onChange={() =>
                  toggleMutation.mutate({ taskId: child.id, isDone: !isDone })
                }
                className="w-4 h-4 rounded border-gray-300 text-indigo-500 focus:ring-indigo-400"
              />
              <span className={`text-sm ${isDone ? 'text-gray-400 line-through' : 'text-gray-700'}`}>
                {child.title}
              </span>
            </label>
          )
        })}
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Verify lint passes**

Run: `cd frontend && npm run lint`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/active-session/SubtaskChecklist.jsx
git commit -m "feat: add SubtaskChecklist component for active session"
```

---

## Task 12: Active Session Page

**Files:**
- Create: `frontend/src/pages/ActiveSessionPage.jsx`

- [ ] **Step 1: Create the main session page**

```jsx
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, useCallback, useEffect } from 'react'
import {
  getScheduleToday,
  startTimeBlock,
  completeTimeBlock,
  doneForNowTimeBlock,
  extendTimeBlock,
} from '@/api/schedule'
import { getTask } from '@/api/tasks'
import { useActiveSession } from '@/contexts/ActiveSessionContext'
import TimerCircle from './active-session/TimerCircle'
import SubtaskChecklist from './active-session/SubtaskChecklist'
import { playCompletionChime } from './active-session/chime'

const TODAY = new Date().toISOString().slice(0, 10)

export default function ActiveSessionPage() {
  const { blockId } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { startSession, clearSession } = useActiveSession()
  const [showExtendMenu, setShowExtendMenu] = useState(false)
  const [flash, setFlash] = useState(null)

  // Fetch today's schedule and find this block
  const { data: blocks } = useQuery({
    queryKey: ['schedule', TODAY],
    queryFn: getScheduleToday,
  })
  const block = blocks?.find((b) => b.id === blockId)

  // Fetch task details (for child tasks) if block has a task
  const { data: taskDetail } = useQuery({
    queryKey: ['tasks', block?.task?.id],
    queryFn: () => getTask(block.task.id),
    enabled: !!block?.task?.id,
  })

  // Calculate timer values from block times
  const endTime = block
    ? new Date(`${block.blockDate}T${block.endTime}`).getTime()
    : null
  const startTime = block
    ? new Date(`${block.blockDate}T${block.startTime}`).getTime()
    : null
  const totalMinutes = block
    ? Math.round((endTime - startTime) / 60000)
    : 0

  // Start the block on mount
  const startMutation = useMutation({
    mutationFn: () => startTimeBlock(blockId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['schedule'] })
    },
  })

  useEffect(() => {
    if (block && !block.actualStart) {
      startMutation.mutate()
    }
  }, [block?.id]) // eslint-disable-line react-hooks/exhaustive-deps

  // Set session context for header timer
  useEffect(() => {
    if (block?.task && endTime) {
      startSession(blockId, block.task.title, endTime)
    }
    return () => {} // Don't clear on unmount — header timer persists
  }, [block?.id, endTime]) // eslint-disable-line react-hooks/exhaustive-deps

  // Complete mutation
  const completeMutation = useMutation({
    mutationFn: () => completeTimeBlock(blockId),
    onSuccess: () => {
      clearSession()
      queryClient.invalidateQueries({ queryKey: ['schedule'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      setFlash('Nice work!')
      setTimeout(() => navigate(-1), 1500)
    },
  })

  // Done for now mutation
  const doneForNowMutation = useMutation({
    mutationFn: () => doneForNowTimeBlock(blockId),
    onSuccess: () => {
      clearSession()
      queryClient.invalidateQueries({ queryKey: ['schedule'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      navigate(-1)
    },
  })

  // Extend mutation
  const extendMutation = useMutation({
    mutationFn: (duration) => extendTimeBlock(blockId, duration),
    onSuccess: (newBlock) => {
      clearSession()
      queryClient.invalidateQueries({ queryKey: ['schedule'] })
      setShowExtendMenu(false)
      navigate(`/session/${newBlock.id}`, { replace: true })
    },
  })

  const handleTimeUp = useCallback(() => {
    playCompletionChime()
  }, [])

  if (!block) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-50">
        <p className="text-gray-400">Loading session...</p>
      </div>
    )
  }

  const children = taskDetail?.children ?? []
  const isPending =
    completeMutation.isPending ||
    doneForNowMutation.isPending ||
    extendMutation.isPending

  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-gradient-to-b from-gray-50 to-indigo-50/30 px-4">
      {/* Flash message */}
      {flash && (
        <div className="fixed top-8 left-1/2 -translate-x-1/2 bg-white shadow-lg rounded-xl px-6 py-3 text-indigo-600 font-medium animate-fade-in z-50">
          {flash}
        </div>
      )}

      {/* Project name */}
      {block.task && (
        <p className="text-xs font-semibold text-gray-400 uppercase tracking-widest mb-2">
          {block.task.projectName}
        </p>
      )}

      {/* Task title */}
      <h1 className="text-xl font-semibold text-gray-800 mb-8 text-center">
        {block.task?.title ?? 'Focus time'}
      </h1>

      {/* Timer */}
      {endTime && (
        <TimerCircle
          endTime={endTime}
          totalMinutes={totalMinutes}
          onTimeUp={handleTimeUp}
        />
      )}

      {/* Subtasks — only rendered if children exist */}
      {children.length > 0 && (
        <div className="mt-8">
          <SubtaskChecklist
            subtasks={children}
            projectId={block.task?.projectId}
          />
        </div>
      )}

      {/* Action buttons */}
      <div className="flex gap-3 mt-8 relative">
        <button
          onClick={() => completeMutation.mutate()}
          disabled={isPending}
          className="px-5 py-2.5 bg-indigo-500 text-white rounded-xl text-sm font-medium hover:bg-indigo-600 transition-colors disabled:opacity-50"
        >
          Complete
        </button>

        <div className="relative">
          <button
            onClick={() => setShowExtendMenu(!showExtendMenu)}
            disabled={isPending}
            className="px-5 py-2.5 bg-indigo-50 text-indigo-600 rounded-xl text-sm font-medium hover:bg-indigo-100 transition-colors disabled:opacity-50"
          >
            Extend
          </button>
          {showExtendMenu && (
            <div className="absolute bottom-full mb-2 left-1/2 -translate-x-1/2 bg-white shadow-lg rounded-xl border border-gray-200 py-1 min-w-[120px]">
              {[15, 30, 60].map((mins) => (
                <button
                  key={mins}
                  onClick={() => extendMutation.mutate(mins)}
                  className="block w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-indigo-50 transition-colors"
                >
                  {mins} min
                </button>
              ))}
            </div>
          )}
        </div>

        <button
          onClick={() => doneForNowMutation.mutate()}
          disabled={isPending}
          className="px-5 py-2.5 bg-indigo-50 text-indigo-600 rounded-xl text-sm font-medium hover:bg-indigo-100 transition-colors disabled:opacity-50"
        >
          Done for now
        </button>
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Check that `getTask` exists in the tasks API**

Look at `frontend/src/api/tasks.js` for a `getTask(taskId)` function. It should exist from Slice 1 (task CRUD). If it doesn't exist, add it:

```javascript
export async function getTask(taskId) {
  const res = await authFetch(`${BASE}/tasks/${taskId}`)
  return handleResponse(res)
}
```

- [ ] **Step 3: Verify lint passes**

Run: `cd frontend && npm run lint`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/ActiveSessionPage.jsx
# Also add tasks.js if it was modified
git commit -m "feat: add ActiveSessionPage with timer, subtasks, and actions"
```

---

## Task 13: Route & Context Wiring

**Files:**
- Modify: `frontend/src/App.jsx`

- [ ] **Step 1: Add route and context provider**

In `App.jsx`, add imports:

```javascript
import { ActiveSessionProvider } from '@/contexts/ActiveSessionContext'
import ActiveSessionPage from '@/pages/ActiveSessionPage'
```

Wrap the existing `AuthProvider` children with `ActiveSessionProvider`. Place it inside `QueryClientProvider` (it needs query access) but outside `Routes`:

```jsx
<QueryClientProvider client={queryClient}>
  <ActiveSessionProvider>
    <Routes>
      {/* ... existing routes ... */}
    </Routes>
  </ActiveSessionProvider>
</QueryClientProvider>
```

Add the session route inside the `ProtectedRoute` element, but **outside** the `AppLayout` element (the session page is full-screen, no sidebar):

```jsx
<Route element={<ProtectedRoute />}>
  <Route path="/session/:blockId" element={<ActiveSessionPage />} />
  <Route element={<AppLayout />}>
    {/* ... existing routes ... */}
  </Route>
</Route>
```

Note: This requires that `ProtectedRoute` renders an `<Outlet />` and does not itself wrap with `AppLayout`. Check the current structure — if `ProtectedRoute` directly wraps `AppLayout`, the route structure may need adjustment. The key requirement: `/session/:blockId` must be inside `ProtectedRoute` but outside `AppLayout`.

- [ ] **Step 2: Verify the app loads**

Run: `cd frontend && npm run dev`
Navigate to `http://localhost:5173/` in a browser. Confirm the dashboard still loads. Navigate to `/session/fake-id` — should show "Loading session..." (since the block won't be found).

- [ ] **Step 3: Commit**

```bash
git add frontend/src/App.jsx
git commit -m "feat: add session route and ActiveSessionProvider"
```

---

## Task 14: Header Timer Widget

**Files:**
- Modify: `frontend/src/layouts/AppLayout.jsx`

- [ ] **Step 1: Add the compact timer widget**

Add import at the top of `AppLayout.jsx`:

```javascript
import { useActiveSession } from '@/contexts/ActiveSessionContext'
import { useNavigate } from 'react-router-dom'
```

Note: `useNavigate` may already be imported. If `Link` is used instead, add `useNavigate`.

Inside the `AppLayout` component, before the return, add:

```javascript
const { session } = useActiveSession()
const navigate = useNavigate()
```

Create a small `HeaderTimer` component (can be defined inside the file or above `AppLayout`):

```jsx
function HeaderTimer({ session, onClick }) {
  const [remaining, setRemaining] = useState('')

  useEffect(() => {
    const tick = () => {
      const ms = session.endTime - Date.now()
      const abs = Math.abs(ms)
      const m = Math.floor(abs / 60000)
      const s = Math.floor((abs % 60000) / 1000)
      const time = `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
      setRemaining(ms < 0 ? `+${time}` : time)
    }
    tick()
    const interval = setInterval(tick, 1000)
    return () => clearInterval(interval)
  }, [session.endTime])

  return (
    <button
      onClick={onClick}
      className="flex items-center gap-2 px-3 py-1.5 bg-indigo-50 text-indigo-700 rounded-lg text-sm font-medium hover:bg-indigo-100 transition-colors"
    >
      <span className="truncate max-w-[150px]">{session.taskName}</span>
      <span className="font-mono tabular-nums">{remaining}</span>
    </button>
  )
}
```

Add `import { useState, useEffect } from 'react'` at the top if not already imported.

In the AppLayout JSX, add the timer widget in the sidebar header area (after the "Planner" logo/brand, before nav items):

```jsx
{session && (
  <HeaderTimer
    session={session}
    onClick={() => navigate(`/session/${session.blockId}`)}
  />
)}
```

Place it so it appears prominently near the top of the sidebar, below the brand name. It should have some margin (e.g., `my-3 mx-3`).

- [ ] **Step 2: Verify lint passes**

Run: `cd frontend && npm run lint`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add frontend/src/layouts/AppLayout.jsx
git commit -m "feat: add persistent timer widget in sidebar"
```

---

## Task 15: Entry Points — Start Buttons

**Files:**
- Modify: `frontend/src/pages/start-day/TimeBlock.jsx`
- Modify: `frontend/src/pages/DashboardPage.jsx`

- [ ] **Step 1: Add Start button to Morning Planning TimeBlock**

In `frontend/src/pages/start-day/TimeBlock.jsx`, add `useNavigate`:

```javascript
import { useNavigate } from 'react-router-dom'
```

Inside the component, add:

```javascript
const navigate = useNavigate()
```

Add a Start button (a play icon or "Start" text) that appears on hover, next to the remove (X) button. Add it inside the block's content area:

```jsx
{block.task?.status !== 'DONE' && (
  <button
    onClick={(e) => {
      e.stopPropagation()
      navigate(`/session/${block.id}`)
    }}
    className="opacity-0 group-hover:opacity-100 transition-opacity text-white/80 hover:text-white text-xs font-medium bg-indigo-600/50 hover:bg-indigo-600/80 rounded px-2 py-0.5"
  >
    Start
  </button>
)}
```

Make sure the parent element has `group` in its className for the hover effect to work. The existing block container likely already uses `group` for the remove button hover behavior — if not, add `group` to the outermost div's className.

- [ ] **Step 2: Add Start button to Dashboard**

In `frontend/src/pages/DashboardPage.jsx`, add a Start button in the "Today at a Glance" card.

First, find the next upcoming (not completed) block. Add to the schedule query:

```javascript
import { getScheduleToday } from '@/api/schedule'
```

Add a query for today's schedule:

```javascript
const { data: todayBlocks } = useQuery({
  queryKey: ['schedule', TODAY],
  queryFn: getScheduleToday,
})
```

Where `TODAY = new Date().toISOString().slice(0, 10)`.

Find the next unstarted block:

```javascript
const nextBlock = todayBlocks?.find(
  (b) => b.task && b.task.status !== 'DONE' && !b.actualEnd
)
```

In the "Today at a Glance" card, after the progress display, add:

```jsx
{nextBlock && (
  <button
    onClick={() => navigate(`/session/${nextBlock.id}`)}
    className="mt-3 w-full px-4 py-2 bg-indigo-500 text-white rounded-lg text-sm font-medium hover:bg-indigo-600 transition-colors"
  >
    Start: {nextBlock.task.title}
  </button>
)}
```

- [ ] **Step 3: Verify lint passes**

Run: `cd frontend && npm run lint`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/start-day/TimeBlock.jsx \
       frontend/src/pages/DashboardPage.jsx
git commit -m "feat: add Start buttons to Morning Planning and Dashboard"
```

---

## Task 16: E2E Tests

**Files:**
- Modify: `e2e/fixtures/data.ts`
- Modify: `e2e/fixtures/mocks.ts`
- Create: `e2e/tests/active-session.spec.ts`

- [ ] **Step 1: Add mock data**

In `e2e/fixtures/data.ts`, add session-ready block data:

```typescript
export const SESSION_BLOCK = {
  id: 'block-session-1',
  blockDate: '2026-04-01',
  startTime: '09:00:00',
  endTime: '09:45:00',
  sortOrder: 0,
  actualStart: null,
  actualEnd: null,
  wasCompleted: false,
  task: {
    id: 'task-1',
    title: 'Write tests',
    projectId: 'proj-1',
    projectName: 'Work',
    projectColor: '#6366f1',
    status: 'TODO',
    pointsEstimate: 2,
  },
}

export const SESSION_TASK_DETAIL = {
  id: 'task-1',
  title: 'Write tests',
  projectId: 'proj-1',
  status: 'TODO',
  children: [
    { id: 'child-1', title: 'Unit tests', status: 'DONE', sortOrder: 0 },
    { id: 'child-2', title: 'Integration tests', status: 'TODO', sortOrder: 1 },
    { id: 'child-3', title: 'E2E tests', status: 'TODO', sortOrder: 2 },
  ],
}
```

- [ ] **Step 2: Add mock helpers**

In `e2e/fixtures/mocks.ts`, add:

```typescript
import { SESSION_BLOCK, SESSION_TASK_DETAIL } from './data'

export async function mockSessionBlock(page: Page, block = SESSION_BLOCK) {
  // Mock schedule endpoint to return this block
  await page.route(/\/api\/v1\/schedule\/today(?!\/)/, (route) =>
    route.fulfill({ json: [block] })
  )
}

export async function mockTaskDetail(page: Page, task = SESSION_TASK_DETAIL) {
  await page.route(/\/api\/v1\/tasks\/[^/]+$/, (route) =>
    route.fulfill({ json: task })
  )
}

export async function mockSessionEndpoints(page: Page) {
  // Start
  await page.route(/\/api\/v1\/time-blocks\/[^/]+\/start/, (route) =>
    route.fulfill({
      json: { ...SESSION_BLOCK, actualStart: new Date().toISOString() },
    })
  )
  // Complete
  await page.route(/\/api\/v1\/time-blocks\/[^/]+\/complete/, (route) =>
    route.fulfill({
      json: {
        ...SESSION_BLOCK,
        actualStart: new Date().toISOString(),
        actualEnd: new Date().toISOString(),
        wasCompleted: true,
        task: { ...SESSION_BLOCK.task, status: 'DONE' },
      },
    })
  )
  // Done for now
  await page.route(/\/api\/v1\/time-blocks\/[^/]+\/done-for-now/, (route) =>
    route.fulfill({
      json: {
        ...SESSION_BLOCK,
        actualStart: new Date().toISOString(),
        actualEnd: new Date().toISOString(),
        wasCompleted: false,
      },
    })
  )
  // Extend
  await page.route(/\/api\/v1\/time-blocks\/[^/]+\/extend/, (route) =>
    route.fulfill({
      json: {
        ...SESSION_BLOCK,
        id: 'block-extended-1',
        startTime: '09:45:00',
        endTime: '10:15:00',
        sortOrder: 1,
      },
    })
  )
}
```

- [ ] **Step 3: Write E2E tests**

Create `e2e/tests/active-session.spec.ts`:

```typescript
import { test, expect } from '../fixtures/auth'
import {
  mockSessionBlock,
  mockTaskDetail,
  mockSessionEndpoints,
  mockDashboard,
} from '../fixtures/mocks'

test.describe('Active Work Session', () => {
  test.beforeEach(async ({ page }) => {
    await mockSessionBlock(page)
    await mockTaskDetail(page)
    await mockSessionEndpoints(page)
  })

  test('shows task title and project name', async ({ page }) => {
    await page.goto('/session/block-session-1')
    await expect(page.getByText('Work')).toBeVisible()
    await expect(page.getByText('Write tests')).toBeVisible()
  })

  test('shows countdown timer', async ({ page }) => {
    await page.goto('/session/block-session-1')
    // Timer should show some time value (format MM:SS)
    await expect(page.getByText(/\d{2}:\d{2}/)).toBeVisible()
    await expect(page.getByText(/of \d+ min/)).toBeVisible()
  })

  test('shows subtask checklist when task has children', async ({ page }) => {
    await page.goto('/session/block-session-1')
    await expect(page.getByText('Subtasks')).toBeVisible()
    await expect(page.getByText('Unit tests')).toBeVisible()
    await expect(page.getByText('Integration tests')).toBeVisible()
    await expect(page.getByText('E2E tests')).toBeVisible()
  })

  test('hides subtask section when task has no children', async ({ page }) => {
    await mockTaskDetail(page, { id: 'task-1', title: 'Write tests', children: [] })
    await page.goto('/session/block-session-1')
    await expect(page.getByText('Write tests')).toBeVisible()
    await expect(page.getByText('Subtasks')).not.toBeVisible()
  })

  test('complete button navigates back with flash', async ({ page }) => {
    await mockDashboard(page)
    await page.goto('/session/block-session-1')
    await page.getByRole('button', { name: 'Complete' }).click()
    await expect(page.getByText('Nice work!')).toBeVisible()
  })

  test('done for now navigates back', async ({ page }) => {
    await mockDashboard(page)
    await page.goto('/session/block-session-1')
    await page.getByRole('button', { name: 'Done for now' }).click()
    // Should navigate away from session page
    await expect(page).not.toHaveURL(/\/session\//)
  })

  test('extend shows duration options', async ({ page }) => {
    await page.goto('/session/block-session-1')
    await page.getByRole('button', { name: 'Extend' }).click()
    await expect(page.getByText('15 min')).toBeVisible()
    await expect(page.getByText('30 min')).toBeVisible()
    await expect(page.getByText('60 min')).toBeVisible()
  })

  test('action buttons are present', async ({ page }) => {
    await page.goto('/session/block-session-1')
    await expect(page.getByRole('button', { name: 'Complete' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Extend' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Done for now' })).toBeVisible()
  })
})
```

- [ ] **Step 4: Run E2E tests**

Run: `cd e2e && npx playwright test active-session`
Expected: All tests PASS

- [ ] **Step 5: Run full E2E suite to check for regressions**

Run: `cd e2e && npx playwright test`
Expected: All tests PASS

- [ ] **Step 6: Commit**

```bash
git add e2e/fixtures/data.ts e2e/fixtures/mocks.ts e2e/tests/active-session.spec.ts
git commit -m "test: add E2E tests for active work session"
```

---

## Task 17: Final Verification & Spec Checklist

- [ ] **Step 1: Run all backend tests**

Run: `cd backend && mvn test`
Expected: All tests PASS

- [ ] **Step 2: Run frontend lint**

Run: `cd frontend && npm run lint`
Expected: No errors

- [ ] **Step 3: Run all E2E tests**

Run: `cd e2e && npx playwright test`
Expected: All tests PASS

- [ ] **Step 4: Check off Slice 5 items in the spec**

Open `docs/planning/2026-03-30-implementation-plan-design.md` and mark all Slice 5 items as checked:

```markdown
- [x] `PATCH /api/v1/time-blocks/{id}/start` — record `actual_start`
- [x] `PATCH /api/v1/time-blocks/{id}/complete` — record `actual_end`, calculate and update `task.actual_minutes`, set `was_completed = true`
- [x] `PATCH /api/v1/time-blocks/{id}/done-for-now` — record `actual_end`, update `task.actual_minutes`, set `was_completed = false`
- [x] Extend: create new adjacent time block for same task
- [x] Active Work Session view — full-screen focus mode: task title, project name, countdown timer, progress bar
- [x] Child tasks as interactive checklist (check to mark done, completion animation)
- [x] Action buttons: Complete / Extend / Done for now
- [x] At 100%: gentle chime + "Time's up. Good work!" message
- [x] Extend prompts for duration (15min, 30min, 1hr)
- [x] Timer persists in header bar if user navigates away
- [x] Entry: click "Start" on a time block from Morning Planning or Dashboard
```

- [ ] **Step 5: Commit spec updates**

```bash
git add docs/planning/2026-03-30-implementation-plan-design.md
git commit -m "docs: check off Slice 5 in implementation spec"
```
