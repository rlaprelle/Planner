# Morning Planning (Slice 4) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the Morning Planning view (`/start-day`) and Dashboard (`/`) — a horizontal drag-and-drop planner with cross-panel task drag, 15-min-snapping block resize, and a four-card dashboard overview.

**Architecture:** New `schedule` backend package owns time blocks. `stats` package gains a dashboard endpoint. Frontend gains a horizontal calendar built on dnd-kit (cross-panel drag) and raw mouse events (resize), with block state managed in `StartDayPage` and utility logic extracted into `useTimeGrid` and `pushBlocks`.

**Tech Stack:** Java 21, Spring Boot 3, Spring Data JPA, Flyway; React 18, Vite, Tailwind CSS, `@dnd-kit/core`, TanStack Query.

---

## File Map

**New backend files:**
- `backend/src/main/resources/db/migration/V6__create_time_block.sql`
- `backend/src/main/java/com/echel/planner/backend/schedule/TimeBlock.java`
- `backend/src/main/java/com/echel/planner/backend/schedule/TimeBlockRepository.java`
- `backend/src/main/java/com/echel/planner/backend/schedule/dto/SavePlanRequest.java`
- `backend/src/main/java/com/echel/planner/backend/schedule/dto/TimeBlockResponse.java`
- `backend/src/main/java/com/echel/planner/backend/schedule/ScheduleService.java`
- `backend/src/main/java/com/echel/planner/backend/schedule/ScheduleController.java`
- `backend/src/main/java/com/echel/planner/backend/schedule/ScheduleExceptionHandler.java`
- `backend/src/main/java/com/echel/planner/backend/stats/dto/DashboardResponse.java`
- `backend/src/test/java/com/echel/planner/backend/schedule/ScheduleControllerIntegrationTest.java`

**Modified backend files:**
- `backend/src/main/java/com/echel/planner/backend/task/TaskRepository.java` — add `findSuggestedForUser`, `findUpcomingDeadlines`
- `backend/src/main/java/com/echel/planner/backend/task/TaskService.java` — add `listSuggested`
- `backend/src/main/java/com/echel/planner/backend/task/TaskController.java` — add `GET /api/v1/tasks/suggested`
- `backend/src/main/java/com/echel/planner/backend/deferred/DeferredItemRepository.java` — add `countPendingForUser`
- `backend/src/main/java/com/echel/planner/backend/stats/StatsService.java` — add `getDashboard`, inject new repos
- `backend/src/main/java/com/echel/planner/backend/stats/StatsController.java` — add `GET /api/v1/stats/dashboard`
- `backend/src/test/java/com/echel/planner/backend/stats/StatsControllerIntegrationTest.java` — add dashboard test

**New frontend files:**
- `frontend/src/api/schedule.js`
- `frontend/src/api/dashboard.js`
- `frontend/src/pages/start-day/pushBlocks.js`
- `frontend/src/pages/start-day/useTimeGrid.js`
- `frontend/src/pages/start-day/TimeBlock.jsx`
- `frontend/src/pages/start-day/TimeBlockGrid.jsx`
- `frontend/src/pages/start-day/TaskCard.jsx`
- `frontend/src/pages/start-day/TaskBrowserRow.jsx`

**Modified frontend files:**
- `frontend/src/pages/StartDayPage.jsx` — replace placeholder
- `frontend/src/pages/DashboardPage.jsx` — replace placeholder

---

## Task 1: Database Migration

**Files:**
- Create: `backend/src/main/resources/db/migration/V6__create_time_block.sql`

- [ ] **Step 1: Create the migration file**

```sql
CREATE TABLE time_block (
    id         UUID    NOT NULL DEFAULT gen_random_uuid(),
    user_id    UUID    NOT NULL,
    block_date DATE    NOT NULL,
    task_id    UUID,
    start_time TIME    NOT NULL,
    end_time   TIME    NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT pk_time_block PRIMARY KEY (id),
    CONSTRAINT fk_time_block_user FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT fk_time_block_task FOREIGN KEY (task_id) REFERENCES task(id)
);

CREATE INDEX idx_time_block_user_date ON time_block(user_id, block_date);
```

- [ ] **Step 2: Start the database and verify the migration runs**

```bash
docker compose up -d
cd backend && mvn flyway:info -q
```

Expected: `V6__create_time_block` shows status `Success`.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/resources/db/migration/V6__create_time_block.sql
git commit -m "feat: add time_block migration (V6)"
```

---

## Task 2: TimeBlock Entity and Repository

**Files:**
- Create: `backend/src/main/java/com/echel/planner/backend/schedule/TimeBlock.java`
- Create: `backend/src/main/java/com/echel/planner/backend/schedule/TimeBlockRepository.java`

- [ ] **Step 1: Create the entity**

```java
package com.echel.planner.backend.schedule;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.task.Task;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "time_block")
public class TimeBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "block_date", nullable = false)
    private LocalDate blockDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public TimeBlock() {}

    public TimeBlock(AppUser user, LocalDate blockDate, Task task, LocalTime startTime, LocalTime endTime, int sortOrder) {
        this.user = user;
        this.blockDate = blockDate;
        this.task = task;
        this.startTime = startTime;
        this.endTime = endTime;
        this.sortOrder = sortOrder;
    }

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public LocalDate getBlockDate() { return blockDate; }
    public Task getTask() { return task; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public int getSortOrder() { return sortOrder; }
}
```

- [ ] **Step 2: Create the repository**

```java
package com.echel.planner.backend.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TimeBlockRepository extends JpaRepository<TimeBlock, UUID> {

    @Query("""
            SELECT tb FROM TimeBlock tb
            LEFT JOIN FETCH tb.task t
            LEFT JOIN FETCH t.project
            WHERE tb.user.id = :userId
              AND tb.blockDate = :blockDate
            ORDER BY tb.sortOrder ASC
            """)
    List<TimeBlock> findByUserIdAndBlockDateWithTask(@Param("userId") UUID userId,
                                                     @Param("blockDate") LocalDate blockDate);

    @Modifying
    @Query("DELETE FROM TimeBlock tb WHERE tb.user.id = :userId AND tb.blockDate = :blockDate")
    void deleteByUserIdAndBlockDate(@Param("userId") UUID userId, @Param("blockDate") LocalDate blockDate);

    @Query("""
            SELECT COUNT(tb) FROM TimeBlock tb
            WHERE tb.user.id = :userId
              AND tb.blockDate = :blockDate
            """)
    long countByUserIdAndBlockDate(@Param("userId") UUID userId, @Param("blockDate") LocalDate blockDate);

    @Query("""
            SELECT COUNT(tb) FROM TimeBlock tb
            WHERE tb.user.id = :userId
              AND tb.blockDate = :blockDate
              AND tb.task.status = com.echel.planner.backend.task.TaskStatus.DONE
            """)
    long countCompletedByUserIdAndBlockDate(@Param("userId") UUID userId, @Param("blockDate") LocalDate blockDate);
}
```

- [ ] **Step 3: Verify compilation**

```bash
cd backend && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/echel/planner/backend/schedule/
git commit -m "feat: add TimeBlock entity and repository"
```

---

## Task 3: Schedule DTOs

**Files:**
- Create: `backend/src/main/java/com/echel/planner/backend/schedule/dto/SavePlanRequest.java`
- Create: `backend/src/main/java/com/echel/planner/backend/schedule/dto/TimeBlockResponse.java`

- [ ] **Step 1: Create `SavePlanRequest`**

```java
package com.echel.planner.backend.schedule.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record SavePlanRequest(
        @NotNull LocalDate blockDate,
        @NotNull List<@Valid BlockEntry> blocks
) {
    public record BlockEntry(
            @NotNull UUID taskId,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime
    ) {}
}
```

- [ ] **Step 2: Create `TimeBlockResponse`**

```java
package com.echel.planner.backend.schedule.dto;

import com.echel.planner.backend.schedule.TimeBlock;
import com.echel.planner.backend.task.TaskStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record TimeBlockResponse(
        UUID id,
        LocalDate blockDate,
        LocalTime startTime,
        LocalTime endTime,
        int sortOrder,
        TaskSummary task
) {
    public record TaskSummary(
            UUID id,
            String title,
            UUID projectId,
            String projectName,
            String projectColor,
            TaskStatus status,
            Short pointsEstimate
    ) {}

    public static TimeBlockResponse from(TimeBlock block) {
        TaskSummary taskSummary = null;
        if (block.getTask() != null) {
            var t = block.getTask();
            taskSummary = new TaskSummary(
                    t.getId(),
                    t.getTitle(),
                    t.getProject().getId(),
                    t.getProject().getName(),
                    t.getProject().getColor(),
                    t.getStatus(),
                    t.getPointsEstimate()
            );
        }
        return new TimeBlockResponse(
                block.getId(),
                block.getBlockDate(),
                block.getStartTime(),
                block.getEndTime(),
                block.getSortOrder(),
                taskSummary
        );
    }
}
```

- [ ] **Step 3: Verify compilation**

```bash
cd backend && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/echel/planner/backend/schedule/dto/
git commit -m "feat: add schedule DTOs (SavePlanRequest, TimeBlockResponse)"
```

---

## Task 4: ScheduleService

**Files:**
- Create: `backend/src/main/java/com/echel/planner/backend/schedule/ScheduleService.java`
- Create: `backend/src/main/java/com/echel/planner/backend/schedule/ScheduleExceptionHandler.java`

- [ ] **Step 1: Create `ScheduleService`**

```java
package com.echel.planner.backend.schedule;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.schedule.dto.SavePlanRequest;
import com.echel.planner.backend.schedule.dto.TimeBlockResponse;
import com.echel.planner.backend.task.Task;
import com.echel.planner.backend.task.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ScheduleService {

    private static final LocalTime DAY_START = LocalTime.of(8, 0);
    private static final LocalTime DAY_END = LocalTime.of(17, 0);

    private final TimeBlockRepository timeBlockRepository;
    private final TaskRepository taskRepository;

    public ScheduleService(TimeBlockRepository timeBlockRepository, TaskRepository taskRepository) {
        this.timeBlockRepository = timeBlockRepository;
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public List<TimeBlockResponse> getToday(AppUser user) {
        LocalDate today = LocalDate.now(ZoneId.of(user.getTimezone()));
        return timeBlockRepository.findByUserIdAndBlockDateWithTask(user.getId(), today)
                .stream()
                .map(TimeBlockResponse::from)
                .toList();
    }

    public List<TimeBlockResponse> savePlan(AppUser user, SavePlanRequest request) {
        validateBlocks(request.blocks());

        timeBlockRepository.deleteByUserIdAndBlockDate(user.getId(), request.blockDate());

        List<TimeBlock> toSave = new ArrayList<>();
        for (int i = 0; i < request.blocks().size(); i++) {
            SavePlanRequest.BlockEntry entry = request.blocks().get(i);
            Task task = taskRepository.findByIdAndUserId(entry.taskId(), user.getId())
                    .orElseThrow(() -> new ScheduleValidationException(
                            "Task not found: " + entry.taskId()));
            toSave.add(new TimeBlock(user, request.blockDate(), task, entry.startTime(), entry.endTime(), i));
        }

        return timeBlockRepository.saveAll(toSave)
                .stream()
                .map(TimeBlockResponse::from)
                .toList();
    }

    private void validateBlocks(List<SavePlanRequest.BlockEntry> blocks) {
        for (int i = 0; i < blocks.size(); i++) {
            SavePlanRequest.BlockEntry b = blocks.get(i);

            if (b.startTime().getMinute() % 15 != 0 || b.startTime().getSecond() != 0) {
                throw new ScheduleValidationException(
                        "Block " + i + ": startTime must be a 15-minute increment (e.g. 09:00, 09:15)");
            }
            if (b.endTime().getMinute() % 15 != 0 || b.endTime().getSecond() != 0) {
                throw new ScheduleValidationException(
                        "Block " + i + ": endTime must be a 15-minute increment");
            }
            if (!b.endTime().isAfter(b.startTime())) {
                throw new ScheduleValidationException(
                        "Block " + i + ": endTime must be after startTime");
            }
            if (b.startTime().isBefore(DAY_START) || b.endTime().isAfter(DAY_END)) {
                throw new ScheduleValidationException(
                        "Block " + i + ": times must be within 08:00–17:00");
            }
        }

        // No overlaps (check sorted order)
        List<SavePlanRequest.BlockEntry> sorted = blocks.stream()
                .sorted(Comparator.comparing(SavePlanRequest.BlockEntry::startTime))
                .toList();
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).startTime().isBefore(sorted.get(i - 1).endTime())) {
                throw new ScheduleValidationException("Blocks must not overlap");
            }
        }
    }

    static class ScheduleValidationException extends RuntimeException {
        ScheduleValidationException(String message) { super(message); }
    }
}
```

- [ ] **Step 2: Create `ScheduleExceptionHandler`**

```java
package com.echel.planner.backend.schedule;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ScheduleExceptionHandler {

    @ExceptionHandler(ScheduleService.ScheduleValidationException.class)
    ProblemDetail handleValidation(ScheduleService.ScheduleValidationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }
}
```

- [ ] **Step 3: Verify compilation**

```bash
cd backend && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/echel/planner/backend/schedule/ScheduleService.java \
        backend/src/main/java/com/echel/planner/backend/schedule/ScheduleExceptionHandler.java
git commit -m "feat: add ScheduleService (savePlan, getToday) with validation"
```

---

## Task 5: ScheduleController and Integration Tests

**Files:**
- Create: `backend/src/main/java/com/echel/planner/backend/schedule/ScheduleController.java`
- Create: `backend/src/test/java/com/echel/planner/backend/schedule/ScheduleControllerIntegrationTest.java`

- [ ] **Step 1: Write the failing tests**

```java
package com.echel.planner.backend.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.auth.JwtAuthFilter;
import com.echel.planner.backend.auth.JwtService;
import com.echel.planner.backend.auth.SecurityConfig;
import com.echel.planner.backend.schedule.dto.SavePlanRequest;
import com.echel.planner.backend.schedule.dto.TimeBlockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScheduleController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthFilter.class, ScheduleExceptionHandler.class})
class ScheduleControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtService jwtService;

    @MockBean AppUserRepository userRepository;
    @MockBean ScheduleService scheduleService;

    private AppUser user;
    private String token;

    @BeforeEach
    void setUp() {
        user = new AppUser("alice@example.com", "hash", "Alice", "UTC");
        token = jwtService.generateAccessToken("alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
    }

    @Test
    void getToday_returnsBlocks() throws Exception {
        UUID taskId = UUID.randomUUID();
        TimeBlockResponse.TaskSummary summary = new TimeBlockResponse.TaskSummary(
                taskId, "Fix login", UUID.randomUUID(), "Auth", "#6366f1",
                com.echel.planner.backend.task.TaskStatus.TODO, (short) 3);
        TimeBlockResponse block = new TimeBlockResponse(
                UUID.randomUUID(), LocalDate.of(2026, 3, 31),
                LocalTime.of(9, 0), LocalTime.of(10, 0), 0, summary);

        when(scheduleService.getToday(any(AppUser.class))).thenReturn(List.of(block));

        mockMvc.perform(get("/api/v1/schedule/today")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$[0].task.title").value("Fix login"));
    }

    @Test
    void getToday_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/schedule/today"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void savePlan_validRequest_returns200WithBlocks() throws Exception {
        UUID taskId = UUID.randomUUID();
        SavePlanRequest request = new SavePlanRequest(
                LocalDate.of(2026, 3, 31),
                List.of(new SavePlanRequest.BlockEntry(taskId, LocalTime.of(9, 0), LocalTime.of(10, 0))));

        TimeBlockResponse.TaskSummary summary = new TimeBlockResponse.TaskSummary(
                taskId, "Fix login", UUID.randomUUID(), "Auth", "#6366f1",
                com.echel.planner.backend.task.TaskStatus.TODO, (short) 3);
        TimeBlockResponse block = new TimeBlockResponse(
                UUID.randomUUID(), LocalDate.of(2026, 3, 31),
                LocalTime.of(9, 0), LocalTime.of(10, 0), 0, summary);

        when(scheduleService.savePlan(any(AppUser.class), any(SavePlanRequest.class)))
                .thenReturn(List.of(block));

        mockMvc.perform(post("/api/v1/schedule/today/plan")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].task.title").value("Fix login"));
    }

    @Test
    void savePlan_validationError_returns422() throws Exception {
        UUID taskId = UUID.randomUUID();
        SavePlanRequest request = new SavePlanRequest(
                LocalDate.of(2026, 3, 31),
                List.of(new SavePlanRequest.BlockEntry(taskId, LocalTime.of(9, 0), LocalTime.of(10, 0))));

        when(scheduleService.savePlan(any(AppUser.class), any(SavePlanRequest.class)))
                .thenThrow(new ScheduleService.ScheduleValidationException("Blocks must not overlap"));

        mockMvc.perform(post("/api/v1/schedule/today/plan")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }
}
```

- [ ] **Step 2: Run tests — expect compilation failure (controller doesn't exist yet)**

```bash
cd backend && mvn test -pl . -Dtest=ScheduleControllerIntegrationTest -q 2>&1 | tail -5
```

Expected: compilation error — `ScheduleController` not found.

- [ ] **Step 3: Create `ScheduleController`**

```java
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
```

- [ ] **Step 4: Run tests — expect pass**

```bash
cd backend && mvn test -Dtest=ScheduleControllerIntegrationTest -q
```

Expected: Tests run: 4, Failures: 0, Errors: 0.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/echel/planner/backend/schedule/ScheduleController.java \
        backend/src/test/java/com/echel/planner/backend/schedule/ScheduleControllerIntegrationTest.java
git commit -m "feat: add ScheduleController with GET today and POST plan endpoints"
```

---

## Task 6: Suggested Tasks Endpoint

**Files:**
- Modify: `backend/src/main/java/com/echel/planner/backend/task/TaskRepository.java`
- Modify: `backend/src/main/java/com/echel/planner/backend/task/TaskService.java`
- Modify: `backend/src/main/java/com/echel/planner/backend/task/TaskController.java`

- [ ] **Step 1: Add `findSuggestedForUser` to `TaskRepository`**

Add this method to the existing `TaskRepository` interface (after the existing `findCompletedTodayForUser` method):

```java
@Query("""
        SELECT DISTINCT t FROM Task t
        JOIN FETCH t.project p
        WHERE t.user.id = :userId
          AND t.status IN (
              com.echel.planner.backend.task.TaskStatus.TODO,
              com.echel.planner.backend.task.TaskStatus.IN_PROGRESS)
          AND t.archivedAt IS NULL
          AND t.parentTask IS NULL
          AND t.id NOT IN (
              SELECT tb.task.id FROM com.echel.planner.backend.schedule.TimeBlock tb
              WHERE tb.user.id = :userId
                AND tb.blockDate = :date
                AND tb.task IS NOT NULL)
        """)
List<Task> findSuggestedForUser(@Param("userId") UUID userId, @Param("date") LocalDate date);
```

Also add the import at the top of `TaskRepository.java`:
```java
import java.time.LocalDate;
```

- [ ] **Step 2: Add `listSuggested` to `TaskService`**

Add this method to the existing `TaskService` class (after `listCompletedToday`):

```java
@Transactional(readOnly = true)
public List<TaskResponse> listSuggested(AppUser user, LocalDate date, int limit) {
    List<Task> tasks = taskRepository.findSuggestedForUser(user.getId(), date);
    return sortAndMap(tasks, user).stream().limit(limit).toList();
}
```

The `sortAndMap` method already exists in `TaskService` — no change needed there.

- [ ] **Step 3: Add the endpoint to `TaskController`**

Add this method to the existing `TaskController` class:

```java
@GetMapping("/api/v1/tasks/suggested")
public ResponseEntity<List<TaskResponse>> listSuggested(
        @AuthenticationPrincipal AppUser user,
        @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam(defaultValue = "50") int limit) {
    LocalDate targetDate = date != null ? date
            : LocalDate.now(java.time.ZoneId.of(user.getTimezone()));
    return ResponseEntity.ok(taskService.listSuggested(user, targetDate, limit));
}
```

Also add this import to `TaskController.java`:
```java
import java.time.LocalDate;
```

- [ ] **Step 4: Verify compilation and run all existing tests**

```bash
cd backend && mvn test -q
```

Expected: BUILD SUCCESS, all existing tests pass.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/echel/planner/backend/task/TaskRepository.java \
        backend/src/main/java/com/echel/planner/backend/task/TaskService.java \
        backend/src/main/java/com/echel/planner/backend/task/TaskController.java
git commit -m "feat: add GET /api/v1/tasks/suggested endpoint"
```

---

## Task 7: Dashboard Stats — Repositories and DTO

**Files:**
- Modify: `backend/src/main/java/com/echel/planner/backend/deferred/DeferredItemRepository.java`
- Modify: `backend/src/main/java/com/echel/planner/backend/task/TaskRepository.java`
- Create: `backend/src/main/java/com/echel/planner/backend/stats/dto/DashboardResponse.java`

- [ ] **Step 1: Add `countPendingForUser` to `DeferredItemRepository`**

Add this method to the existing `DeferredItemRepository` interface:

```java
@Query("""
        SELECT COUNT(d) FROM DeferredItem d
        WHERE d.user.id = :userId
          AND d.isProcessed = false
          AND (d.deferredUntilDate IS NULL OR d.deferredUntilDate <= :today)
        """)
long countPendingForUser(@Param("userId") UUID userId, @Param("today") java.time.LocalDate today);
```

- [ ] **Step 2: Add `findUpcomingDeadlines` to `TaskRepository`**

Add this method to the existing `TaskRepository` interface:

```java
@Query("""
        SELECT t FROM Task t
        JOIN FETCH t.project p
        WHERE t.user.id = :userId
          AND t.dueDate IS NOT NULL
          AND t.dueDate >= :today
          AND t.status != com.echel.planner.backend.task.TaskStatus.DONE
          AND t.archivedAt IS NULL
          AND t.parentTask IS NULL
        ORDER BY t.dueDate ASC
        """)
List<Task> findUpcomingDeadlines(@Param("userId") UUID userId,
                                  @Param("today") LocalDate today,
                                  org.springframework.data.domain.Pageable pageable);
```

- [ ] **Step 3: Create `DashboardResponse`**

```java
package com.echel.planner.backend.stats.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DashboardResponse(
        int todayBlockCount,
        int todayCompletedCount,
        int streakDays,
        List<DeadlineSummary> upcomingDeadlines,
        int deferredItemCount
) {
    public record DeadlineSummary(
            UUID taskId,
            String taskTitle,
            String projectName,
            String projectColor,
            LocalDate dueDate,
            String deadlineGroup
    ) {}
}
```

- [ ] **Step 4: Verify compilation**

```bash
cd backend && mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/echel/planner/backend/deferred/DeferredItemRepository.java \
        backend/src/main/java/com/echel/planner/backend/task/TaskRepository.java \
        backend/src/main/java/com/echel/planner/backend/stats/dto/DashboardResponse.java
git commit -m "feat: add dashboard query methods and DashboardResponse DTO"
```

---

## Task 8: StatsService Dashboard + Controller + Tests

**Files:**
- Modify: `backend/src/main/java/com/echel/planner/backend/stats/StatsService.java`
- Modify: `backend/src/main/java/com/echel/planner/backend/stats/StatsController.java`
- Modify: `backend/src/test/java/com/echel/planner/backend/stats/StatsControllerIntegrationTest.java`

- [ ] **Step 1: Update `StatsService` to add `getDashboard`**

Replace the entire `StatsService.java` with:

```java
package com.echel.planner.backend.stats;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.deferred.DeferredItemRepository;
import com.echel.planner.backend.reflection.DailyReflectionRepository;
import com.echel.planner.backend.schedule.TimeBlockRepository;
import com.echel.planner.backend.stats.dto.DashboardResponse;
import com.echel.planner.backend.task.Task;
import com.echel.planner.backend.task.TaskRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class StatsService {

    private final DailyReflectionRepository reflectionRepository;
    private final TimeBlockRepository timeBlockRepository;
    private final TaskRepository taskRepository;
    private final DeferredItemRepository deferredItemRepository;

    public StatsService(DailyReflectionRepository reflectionRepository,
                        TimeBlockRepository timeBlockRepository,
                        TaskRepository taskRepository,
                        DeferredItemRepository deferredItemRepository) {
        this.reflectionRepository = reflectionRepository;
        this.timeBlockRepository = timeBlockRepository;
        this.taskRepository = taskRepository;
        this.deferredItemRepository = deferredItemRepository;
    }

    public int getStreak(AppUser user) {
        return computeStreak(user, false);
    }

    public DashboardResponse getDashboard(AppUser user) {
        LocalDate today = LocalDate.now(ZoneId.of(user.getTimezone()));

        long blockCount = timeBlockRepository.countByUserIdAndBlockDate(user.getId(), today);
        long completedCount = timeBlockRepository.countCompletedByUserIdAndBlockDate(user.getId(), today);

        int streak = computeStreak(user, true);

        List<Task> upcoming = taskRepository.findUpcomingDeadlines(user.getId(), today, PageRequest.of(0, 5));
        LocalDate endOfWeek = today.plusDays(7);
        List<DashboardResponse.DeadlineSummary> deadlines = upcoming.stream()
                .map(t -> new DashboardResponse.DeadlineSummary(
                        t.getId(),
                        t.getTitle(),
                        t.getProject().getName(),
                        t.getProject().getColor(),
                        t.getDueDate(),
                        computeDeadlineGroup(t.getDueDate(), today, endOfWeek)))
                .toList();

        long deferredCount = deferredItemRepository.countPendingForUser(user.getId(), today);

        return new DashboardResponse(
                (int) blockCount,
                (int) completedCount,
                streak,
                deadlines,
                (int) deferredCount);
    }

    /**
     * Counts consecutive finalized-reflection days ending today.
     * If allowYesterdayFallback is true and today has no reflection,
     * counts from yesterday — useful for dashboard display during the day.
     */
    private int computeStreak(AppUser user, boolean allowYesterdayFallback) {
        List<LocalDate> dates = reflectionRepository.findFinalizedDatesDesc(user);
        LocalDate today = LocalDate.now(ZoneId.of(user.getTimezone()));

        LocalDate expected = today;
        if (allowYesterdayFallback && (dates.isEmpty() || !dates.get(0).equals(today))) {
            expected = today.minusDays(1);
        }

        int streak = 0;
        for (LocalDate date : dates) {
            if (date.equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else {
                break;
            }
        }
        return streak;
    }

    private String computeDeadlineGroup(LocalDate dueDate, LocalDate today, LocalDate endOfWeek) {
        if (dueDate == null || dueDate.isAfter(endOfWeek)) return "NO_DEADLINE";
        if (!dueDate.isAfter(today)) return "TODAY";
        return "THIS_WEEK";
    }
}
```

- [ ] **Step 2: Add `getDashboard` endpoint to `StatsController`**

Add this method to the existing `StatsController` class:

```java
@GetMapping("/dashboard")
public ResponseEntity<DashboardResponse> dashboard(@AuthenticationPrincipal AppUser user) {
    return ResponseEntity.ok(statsService.getDashboard(user));
}
```

Also add this import at the top of `StatsController.java`:
```java
import com.echel.planner.backend.stats.dto.DashboardResponse;
```

- [ ] **Step 3: Add dashboard test to `StatsControllerIntegrationTest`**

Add this test method to the existing `StatsControllerIntegrationTest` class:

```java
@Test
void dashboard_returnsAggregatedData() throws Exception {
    DashboardResponse response = new DashboardResponse(
            4, 1, 7,
            List.of(new DashboardResponse.DeadlineSummary(
                    java.util.UUID.randomUUID(), "Fix login", "Auth", "#6366f1",
                    java.time.LocalDate.of(2026, 3, 31), "TODAY")),
            3);

    when(statsService.getDashboard(any(AppUser.class))).thenReturn(response);

    mockMvc.perform(get("/api/v1/stats/dashboard")
                    .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.todayBlockCount").value(4))
            .andExpect(jsonPath("$.streakDays").value(7))
            .andExpect(jsonPath("$.upcomingDeadlines[0].taskTitle").value("Fix login"))
            .andExpect(jsonPath("$.deferredItemCount").value(3));
}
```

Also add these imports to the test file:
```java
import com.echel.planner.backend.stats.dto.DashboardResponse;
import java.util.List;
```

- [ ] **Step 4: Run all backend tests**

```bash
cd backend && mvn test -q
```

Expected: BUILD SUCCESS, all tests pass.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/echel/planner/backend/stats/StatsService.java \
        backend/src/main/java/com/echel/planner/backend/stats/StatsController.java \
        backend/src/test/java/com/echel/planner/backend/stats/StatsControllerIntegrationTest.java
git commit -m "feat: add GET /api/v1/stats/dashboard endpoint"
```

---

## Task 9: Frontend Setup — dnd-kit and API Layer

**Files:**
- Modify: `frontend/package.json` (via npm install)
- Create: `frontend/src/api/schedule.js`
- Create: `frontend/src/api/dashboard.js`

- [ ] **Step 1: Install dnd-kit**

```bash
cd frontend && npm install @dnd-kit/core @dnd-kit/utilities
```

Expected: `package.json` now includes `"@dnd-kit/core"` and `"@dnd-kit/utilities"` in dependencies.

- [ ] **Step 2: Create `frontend/src/api/schedule.js`**

```javascript
import { authFetch, handleResponse } from './client'

const BASE = '/api/v1'

export async function getScheduleToday() {
  const res = await authFetch(`${BASE}/schedule/today`)
  return handleResponse(res)
}

export async function savePlan(blockDate, blocks) {
  const res = await authFetch(`${BASE}/schedule/today/plan`, {
    method: 'POST',
    body: JSON.stringify({ blockDate, blocks }),
  })
  return handleResponse(res)
}

export async function getSuggestedTasks(date, limit = 50) {
  const params = new URLSearchParams({ date, limit })
  const res = await authFetch(`${BASE}/tasks/suggested?${params}`)
  return handleResponse(res)
}
```

- [ ] **Step 3: Create `frontend/src/api/dashboard.js`**

```javascript
import { authFetch, handleResponse } from './client'

export async function getDashboard() {
  const res = await authFetch('/api/v1/stats/dashboard')
  return handleResponse(res)
}
```

- [ ] **Step 4: Verify lint passes**

```bash
cd frontend && npm run lint
```

Expected: no errors.

- [ ] **Step 5: Commit**

```bash
git add frontend/package.json frontend/package-lock.json \
        frontend/src/api/schedule.js frontend/src/api/dashboard.js
git commit -m "feat: add dnd-kit and frontend API files for schedule and dashboard"
```

---

## Task 10: pushBlocks Utility

**Files:**
- Create: `frontend/src/pages/start-day/pushBlocks.js`

This is a pure-function module — no React, no side effects. It handles all time math.

- [ ] **Step 1: Create `frontend/src/pages/start-day/pushBlocks.js`**

```javascript
/**
 * Convert "HH:MM" or "HH:MM:SS" string to minutes from midnight.
 * @param {string} timeStr
 * @returns {number}
 */
export function timeToMinutes(timeStr) {
  const parts = timeStr.split(':').map(Number)
  return parts[0] * 60 + parts[1]
}

/**
 * Convert minutes from midnight to "HH:MM" string.
 * @param {number} minutes
 * @returns {string}
 */
export function minutesToTime(minutes) {
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
}

/**
 * Snap a minute value to the nearest 15-minute increment.
 * @param {number} minutes
 * @returns {number}
 */
export function snapTo15(minutes) {
  return Math.round(minutes / 15) * 15
}

/**
 * Pure function. Given a sorted array of grid blocks (sorted by startMinutes)
 * and the index of the block that just changed, pushes subsequent overlapping
 * blocks forward to eliminate overlap.
 *
 * Returns a new array (does not mutate input).
 * Returns null if the push chain would cause the last block to exceed dayEndMinutes.
 *
 * @param {Array<{id: string, startMinutes: number, endMinutes: number}>} blocks - sorted by startMinutes
 * @param {number} changedIndex - index of the block that triggered the push
 * @param {number} dayEndMinutes - e.g. 17 * 60 = 1020 for 5 PM
 * @returns {Array|null}
 */
export function pushBlocks(blocks, changedIndex, dayEndMinutes) {
  const result = blocks.map(b => ({ ...b }))

  for (let i = changedIndex + 1; i < result.length; i++) {
    const prev = result[i - 1]
    const curr = result[i]
    if (curr.startMinutes < prev.endMinutes) {
      const shift = prev.endMinutes - curr.startMinutes
      result[i] = {
        ...curr,
        startMinutes: curr.startMinutes + shift,
        endMinutes: curr.endMinutes + shift,
      }
    } else {
      break // gap exists, no further pushing needed
    }
  }

  // Reject if the last block exceeds the day end
  if (result.length > 0 && result[result.length - 1].endMinutes > dayEndMinutes) {
    return null
  }

  return result
}

/**
 * Convert a server TimeBlockResponse to a grid block with minute fields.
 * @param {Object} serverBlock - has startTime "HH:MM:SS", endTime "HH:MM:SS"
 * @returns {Object}
 */
export function toGridBlock(serverBlock) {
  return {
    ...serverBlock,
    startMinutes: timeToMinutes(serverBlock.startTime),
    endMinutes: timeToMinutes(serverBlock.endTime),
  }
}
```

- [ ] **Step 2: Verify lint**

```bash
cd frontend && npm run lint -- src/pages/start-day/pushBlocks.js
```

Expected: no errors.

- [ ] **Step 3: Manually verify `pushBlocks` logic in browser console or Node**

Open the browser console (or run `node`) and paste:

```javascript
// simulate: import { pushBlocks, snapTo15, timeToMinutes, minutesToTime } from './pushBlocks.js'
// Manual smoke test
const blocks = [
  { id: 'a', startMinutes: 540, endMinutes: 600 },  // 9:00–10:00
  { id: 'b', startMinutes: 600, endMinutes: 660 },  // 10:00–11:00
  { id: 'c', startMinutes: 660, endMinutes: 720 },  // 11:00–12:00
]
// Move block 0 to end at 630 (10:30)
blocks[0].endMinutes = 630
const result = pushBlocks(blocks, 0, 1020)
console.log(result[1].startMinutes) // expect 630
console.log(result[2].startMinutes) // expect 690
console.log(result[2].endMinutes)   // expect 750

// Test cap: move block 0 to end at 1000, last block would exceed 1020
blocks[0].endMinutes = 1000
const capped = pushBlocks(blocks, 0, 1020)
console.log(capped) // expect null
```

Expected: values match comments above.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/start-day/pushBlocks.js
git commit -m "feat: add pushBlocks time utility (pure functions)"
```

---

## Task 11: useTimeGrid Hook

**Files:**
- Create: `frontend/src/pages/start-day/useTimeGrid.js`

- [ ] **Step 1: Create `frontend/src/pages/start-day/useTimeGrid.js`**

```javascript
import { useRef } from 'react'
import { pushBlocks, snapTo15, minutesToTime, timeToMinutes } from './pushBlocks'

export const DAY_START_MINUTES = 8 * 60   // 480 — 8 AM
export const DAY_END_MINUTES = 17 * 60    // 1020 — 5 PM
export const DAY_DURATION = DAY_END_MINUTES - DAY_START_MINUTES // 540

/**
 * Provides the grid ref, coordinate helpers, and resize handler.
 * Block state is owned by the caller (StartDayPage).
 */
export function useTimeGrid() {
  const gridRef = useRef(null)
  const resizeRef = useRef(null)

  /**
   * Convert minutes-from-midnight to a percentage left offset within the grid.
   */
  function minutesToPercent(minutes) {
    return ((minutes - DAY_START_MINUTES) / DAY_DURATION) * 100
  }

  /**
   * Convert a duration in minutes to a percentage width within the grid.
   */
  function durationToPercent(durationMinutes) {
    return (durationMinutes / DAY_DURATION) * 100
  }

  /**
   * Convert a pixel delta (e.g. from drag) to minutes.
   */
  function pixelDeltaToMinutes(deltaX) {
    if (!gridRef.current) return 0
    const { width } = gridRef.current.getBoundingClientRect()
    return (deltaX / width) * DAY_DURATION
  }

  /**
   * Convert an absolute clientX position to minutes-from-midnight,
   * relative to the grid's left edge.
   */
  function clientXToMinutes(clientX) {
    if (!gridRef.current) return DAY_START_MINUTES
    const { left, width } = gridRef.current.getBoundingClientRect()
    const ratio = Math.max(0, Math.min(1, (clientX - left) / width))
    return DAY_START_MINUTES + ratio * DAY_DURATION
  }

  /**
   * Begin a resize drag on a block's right edge.
   *
   * @param {MouseEvent} e - the mousedown event on the resize handle
   * @param {Object} block - the block being resized (must have startMinutes, endMinutes, id)
   * @param {number} blockIndex - index of the block in the `blocks` array
   * @param {Array} blocks - the current full blocks array
   * @param {Function} onBlocksChange - called with the new blocks array on each tick
   */
  function startResize(e, block, blockIndex, blocks, onBlocksChange) {
    e.preventDefault()
    e.stopPropagation()

    resizeRef.current = {
      blockIndex,
      initialClientX: e.clientX,
      initialEndMinutes: block.endMinutes,
      blocks,
      onBlocksChange,
    }

    function onMove(moveEvent) {
      const ref = resizeRef.current
      if (!ref) return

      const deltaX = moveEvent.clientX - ref.initialClientX
      const deltaMins = pixelDeltaToMinutes(deltaX)
      const rawEnd = ref.initialEndMinutes + deltaMins
      const minEnd = ref.blocks[ref.blockIndex].startMinutes + 15
      const snappedEnd = Math.max(minEnd, snapTo15(rawEnd))

      const newBlocks = ref.blocks.map((b, i) =>
        i === ref.blockIndex ? { ...b, endMinutes: snappedEnd } : { ...b }
      )

      const pushed = pushBlocks(newBlocks, ref.blockIndex, DAY_END_MINUTES)
      if (pushed) {
        ref.onBlocksChange(pushed)
      }
      // If pushed is null, the resize is capped — don't update
    }

    function onUp() {
      resizeRef.current = null
      window.removeEventListener('mousemove', onMove)
      window.removeEventListener('mouseup', onUp)
    }

    window.addEventListener('mousemove', onMove)
    window.addEventListener('mouseup', onUp)
  }

  return {
    gridRef,
    minutesToPercent,
    durationToPercent,
    pixelDeltaToMinutes,
    clientXToMinutes,
    startResize,
  }
}
```

- [ ] **Step 2: Verify lint**

```bash
cd frontend && npm run lint -- src/pages/start-day/useTimeGrid.js
```

Expected: no errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/start-day/useTimeGrid.js
git commit -m "feat: add useTimeGrid hook (coordinate math, resize handler)"
```

---

## Task 12: TimeBlock and TimeBlockGrid Components

**Files:**
- Create: `frontend/src/pages/start-day/TimeBlock.jsx`
- Create: `frontend/src/pages/start-day/TimeBlockGrid.jsx`

- [ ] **Step 1: Create `frontend/src/pages/start-day/TimeBlock.jsx`**

```jsx
import { useDraggable } from '@dnd-kit/core'

/**
 * A single draggable, resizable time block on the calendar grid.
 *
 * Props:
 *   block          - { id, startMinutes, endMinutes, task: { title, status } }
 *   blockIndex     - index in the blocks array
 *   allBlocks      - the full blocks array (passed to startResize)
 *   onBlocksChange - called when blocks change via resize
 *   minutesToPercent(n)    - grid helper from useTimeGrid
 *   durationToPercent(n)   - grid helper from useTimeGrid
 *   startResize(e, block, index, blocks, onChange) - from useTimeGrid
 */
export function TimeBlock({
  block,
  blockIndex,
  allBlocks,
  onBlocksChange,
  minutesToPercent,
  durationToPercent,
  startResize,
}) {
  const isCompleted = block.task?.status === 'DONE'

  const { attributes, listeners, setNodeRef, transform, isDragging } = useDraggable({
    id: `calendar-block-${block.id}`,
    data: { type: 'calendar-block', block, blockIndex },
    disabled: isCompleted,
  })

  const left = minutesToPercent(block.startMinutes)
  const width = durationToPercent(block.endMinutes - block.startMinutes)

  const style = {
    position: 'absolute',
    left: `${left}%`,
    width: `${Math.max(width, 1)}%`,
    top: '3px',
    bottom: '3px',
    transform: transform ? `translateX(${transform.x}px)` : undefined,
    opacity: isDragging ? 0.4 : 1,
    zIndex: isDragging ? 20 : 1,
    cursor: isCompleted ? 'default' : isDragging ? 'grabbing' : 'grab',
    userSelect: 'none',
  }

  const startLabel = `${Math.floor(block.startMinutes / 60)}:${String(block.startMinutes % 60).padStart(2, '0')}`
  const endLabel = `${Math.floor(block.endMinutes / 60)}:${String(block.endMinutes % 60).padStart(2, '0')}`

  return (
    <div
      ref={setNodeRef}
      style={style}
      className={`rounded flex items-center overflow-hidden select-none border ${
        isCompleted
          ? 'bg-gray-100 border-gray-200 text-gray-400'
          : 'bg-indigo-500 border-indigo-600 text-white'
      }`}
      {...(isCompleted ? {} : { ...listeners, ...attributes })}
    >
      {/* Block body — shows title and time */}
      <div className="flex-1 flex items-center gap-1 px-2 overflow-hidden min-w-0">
        <span className="text-xs font-medium truncate">
          {block.task?.title ?? 'Untitled'}
        </span>
        <span className={`text-xs shrink-0 ${isCompleted ? 'text-gray-400' : 'text-indigo-200'}`}>
          {startLabel}–{endLabel}
        </span>
      </div>

      {/* Resize handle — right edge */}
      {!isCompleted && (
        <div
          className="absolute right-0 top-0 bottom-0 w-2 cursor-ew-resize hover:bg-white/20 rounded-r"
          onMouseDown={(e) => startResize(e, block, blockIndex, allBlocks, onBlocksChange)}
        />
      )}
    </div>
  )
}
```

- [ ] **Step 2: Create `frontend/src/pages/start-day/TimeBlockGrid.jsx`**

```jsx
import { useDroppable } from '@dnd-kit/core'
import { TimeBlock } from './TimeBlock'
import { DAY_START_MINUTES, DAY_END_MINUTES } from './useTimeGrid'

// Hours to display: 8 AM through 5 PM
const HOURS = Array.from(
  { length: DAY_END_MINUTES / 60 - DAY_START_MINUTES / 60 + 1 },
  (_, i) => i + DAY_START_MINUTES / 60
) // [8, 9, 10, 11, 12, 13, 14, 15, 16, 17]

function formatHour(h) {
  if (h === 12) return '12 PM'
  if (h < 12) return `${h} AM`
  return `${h - 12} PM`
}

/**
 * The horizontal planner grid.
 *
 * Props:
 *   blocks          - array of grid blocks (with startMinutes, endMinutes)
 *   onBlocksChange  - called with new blocks array
 *   gridRef         - ref from useTimeGrid (attach to the body area)
 *   minutesToPercent, durationToPercent, startResize - from useTimeGrid
 */
export function TimeBlockGrid({
  blocks,
  onBlocksChange,
  gridRef,
  minutesToPercent,
  durationToPercent,
  startResize,
}) {
  const { setNodeRef, isOver } = useDroppable({ id: 'time-block-grid' })

  // Merge the droppable ref and the grid measurement ref
  const mergedRef = (el) => {
    setNodeRef(el)
    gridRef.current = el
  }

  return (
    <div
      className={`relative border border-gray-200 rounded-lg overflow-hidden transition-colors ${
        isOver ? 'bg-indigo-50' : 'bg-white'
      }`}
    >
      {/* Hour columns with time labels and grid lines */}
      <div className="flex">
        {HOURS.map((hour, idx) => (
          <div
            key={hour}
            className={`flex-1 border-r border-gray-200 ${idx === HOURS.length - 1 ? 'border-r-0' : ''}`}
          >
            <div className="text-xs text-gray-400 px-1 py-1 border-b border-gray-100 whitespace-nowrap">
              {formatHour(hour)}
            </div>
            {/* 15-min sub-lines inside the body */}
            <div className="h-14 relative">
              <div className="absolute left-1/4 top-0 bottom-0 border-l border-gray-100" />
              <div className="absolute left-2/4 top-0 bottom-0 border-l border-gray-200" />
              <div className="absolute left-3/4 top-0 bottom-0 border-l border-gray-100" />
            </div>
          </div>
        ))}
      </div>

      {/* Absolute overlay for time blocks — uses gridRef for pixel↔time conversion */}
      <div
        ref={mergedRef}
        className="absolute left-0 right-0 bottom-0"
        style={{ top: '1.75rem' }} /* height of the time label row */
      >
        {blocks.map((block, i) => (
          <TimeBlock
            key={block.id}
            block={block}
            blockIndex={i}
            allBlocks={blocks}
            onBlocksChange={onBlocksChange}
            minutesToPercent={minutesToPercent}
            durationToPercent={durationToPercent}
            startResize={startResize}
          />
        ))}
      </div>
    </div>
  )
}
```

- [ ] **Step 3: Verify lint**

```bash
cd frontend && npm run lint -- src/pages/start-day/
```

Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/start-day/TimeBlock.jsx \
        frontend/src/pages/start-day/TimeBlockGrid.jsx
git commit -m "feat: add TimeBlock and TimeBlockGrid components"
```

---

## Task 13: TaskCard and TaskBrowserRow Components

**Files:**
- Create: `frontend/src/pages/start-day/TaskCard.jsx`
- Create: `frontend/src/pages/start-day/TaskBrowserRow.jsx`

- [ ] **Step 1: Create `frontend/src/pages/start-day/TaskCard.jsx`**

A single draggable task card used in all three browser rows.

```jsx
import { useDraggable } from '@dnd-kit/core'

const DEADLINE_BADGE = {
  TODAY: { label: 'TODAY', className: 'bg-red-100 text-red-700' },
  THIS_WEEK: { label: 'THIS WK', className: 'bg-amber-100 text-amber-700' },
}

/**
 * A single task card shown in the task browser rows.
 *
 * Props:
 *   task        - TaskResponse from server
 *   isSelected  - whether the checkbox is checked
 *   isScheduled - task is already on the calendar (checkbox disabled)
 *   onToggle(taskId) - called when checkbox changes
 */
export function TaskCard({ task, isSelected, isScheduled, onToggle }) {
  const { attributes, listeners, setNodeRef, isDragging } = useDraggable({
    id: `task-card-${task.id}`,
    data: { type: 'task-card', task },
    disabled: isScheduled,
  })

  const badge = DEADLINE_BADGE[task.deadlineGroup]

  return (
    <div
      ref={setNodeRef}
      className={`flex items-center gap-2 bg-white rounded px-2 py-1.5 border text-xs
        ${isDragging ? 'opacity-40 border-indigo-300' : 'border-gray-200'}
        ${isScheduled ? 'opacity-50' : 'cursor-grab hover:border-indigo-300 hover:shadow-sm'}
        transition-all`}
      {...(isScheduled ? {} : { ...listeners, ...attributes })}
    >
      <input
        type="checkbox"
        checked={isSelected || isScheduled}
        disabled={isScheduled}
        onChange={() => onToggle(task.id)}
        onClick={(e) => e.stopPropagation()}
        className="shrink-0 accent-indigo-600"
      />
      <span className="truncate flex-1 text-gray-800">{task.title}</span>
      {badge && (
        <span className={`shrink-0 rounded px-1 py-0.5 text-[10px] font-semibold ${badge.className}`}>
          {badge.label}
        </span>
      )}
      {task.pointsEstimate != null && (
        <span className="shrink-0 text-gray-400">{task.pointsEstimate}pt</span>
      )}
    </div>
  )
}
```

- [ ] **Step 2: Create `frontend/src/pages/start-day/TaskBrowserRow.jsx`**

Renders a horizontally scrollable set of project columns for a given task list. Used for all three rows (all tasks, due today, due this week).

```jsx
import { TaskCard } from './TaskCard'

/**
 * A horizontally scrollable row of project columns, each showing tasks for that project.
 *
 * Props:
 *   tasks           - array of TaskResponse (already filtered for this row)
 *   selectedTaskIds - Set<string>
 *   scheduledTaskIds - Set<string> (tasks already on calendar)
 *   onToggle(taskId) - toggle selection
 *   emptyMessage    - string shown when a project has no tasks matching this row's filter
 */
export function TaskBrowserRow({ tasks, selectedTaskIds, scheduledTaskIds, onToggle, emptyMessage = 'Nothing here' }) {
  // Group tasks by project, maintaining insertion order
  const projectMap = new Map()
  for (const task of tasks) {
    const key = task.projectId
    if (!projectMap.has(key)) {
      projectMap.set(key, { name: task.projectName ?? 'Unknown', color: task.projectColor, tasks: [] })
    }
    projectMap.get(key).tasks.push(task)
  }

  if (projectMap.size === 0) {
    return (
      <p className="text-xs text-gray-400 italic px-1">{emptyMessage}</p>
    )
  }

  return (
    <div className="flex gap-3 overflow-x-auto pb-1">
      {[...projectMap.entries()].map(([projectId, { name, color, tasks: ptasks }]) => (
        <div
          key={projectId}
          className="min-w-[130px] max-w-[160px] flex-shrink-0 bg-indigo-50 rounded-lg p-2"
        >
          {/* Project header */}
          <div className="flex items-center gap-1.5 mb-2">
            {color && (
              <span
                className="inline-block w-2.5 h-2.5 rounded-full shrink-0"
                style={{ background: color }}
              />
            )}
            <span className="text-xs font-bold text-indigo-800 truncate">{name}</span>
          </div>

          {/* Task cards */}
          <div className="flex flex-col gap-1">
            {ptasks.map((task) => (
              <TaskCard
                key={task.id}
                task={task}
                isSelected={selectedTaskIds.has(task.id)}
                isScheduled={scheduledTaskIds.has(task.id)}
                onToggle={onToggle}
              />
            ))}
          </div>
        </div>
      ))}
      {/* Scroll affordance */}
      <div className="w-4 shrink-0" />
    </div>
  )
}
```

- [ ] **Step 3: Verify lint**

```bash
cd frontend && npm run lint -- src/pages/start-day/
```

Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/start-day/TaskCard.jsx \
        frontend/src/pages/start-day/TaskBrowserRow.jsx
git commit -m "feat: add TaskCard and TaskBrowserRow components"
```

---

## Task 14: StartDayPage

**Files:**
- Modify: `frontend/src/pages/StartDayPage.jsx`

- [ ] **Step 1: Replace the placeholder with the full implementation**

```jsx
import { useState, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { DndContext, DragOverlay, PointerSensor, useSensor, useSensors } from '@dnd-kit/core'
import { format } from 'date-fns'

import { getSuggestedTasks, getScheduleToday, savePlan } from '@/api/schedule'
import { useTimeGrid, DAY_START_MINUTES, DAY_END_MINUTES } from './start-day/useTimeGrid'
import { pushBlocks, toGridBlock, snapTo15, minutesToTime } from './start-day/pushBlocks'
import { TimeBlockGrid } from './start-day/TimeBlockGrid'
import { TaskBrowserRow } from './start-day/TaskBrowserRow'
import { TaskCard } from './start-day/TaskCard'

const TODAY = format(new Date(), 'yyyy-MM-dd')

export function StartDayPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  // --- Grid helpers ---
  const { gridRef, minutesToPercent, durationToPercent, pixelDeltaToMinutes, clientXToMinutes, startResize } =
    useTimeGrid()

  // --- Server data ---
  const { data: suggestedTasks = [], isLoading: loadingTasks } = useQuery({
    queryKey: ['tasks', 'suggested', TODAY],
    queryFn: () => getSuggestedTasks(TODAY),
  })

  const { data: existingBlocks = [] } = useQuery({
    queryKey: ['schedule', TODAY],
    queryFn: getScheduleToday,
    select: (data) => data.map(toGridBlock),
  })

  // --- Local state ---
  const [blocks, setBlocks] = useState(null) // null = not yet initialised from server
  const [selectedTaskIds, setSelectedTaskIds] = useState(new Set())
  const [addWarning, setAddWarning] = useState(null)
  const [activeTaskCard, setActiveTaskCard] = useState(null) // for DragOverlay

  // Initialise blocks from server once (mid-day replanning support)
  const gridBlocks = blocks ?? existingBlocks

  const scheduledTaskIds = useMemo(
    () => new Set(gridBlocks.map((b) => b.task?.id).filter(Boolean)),
    [gridBlocks]
  )

  // --- Filtered task lists for deadline rows ---
  const dueTodayTasks = useMemo(
    () => suggestedTasks.filter((t) => t.deadlineGroup === 'TODAY'),
    [suggestedTasks]
  )
  const dueThisWeekTasks = useMemo(
    () => suggestedTasks.filter((t) => t.deadlineGroup === 'THIS_WEEK'),
    [suggestedTasks]
  )

  // --- Selection ---
  function toggleTask(taskId) {
    setSelectedTaskIds((prev) => {
      const next = new Set(prev)
      if (next.has(taskId)) next.delete(taskId)
      else next.add(taskId)
      return next
    })
  }

  // --- Add selected tasks to calendar ---
  function handleAddToCalendar() {
    setAddWarning(null)
    const toAdd = suggestedTasks.filter(
      (t) => selectedTaskIds.has(t.id) && !scheduledTaskIds.has(t.id)
    )
    const lastEnd =
      gridBlocks.length > 0 ? Math.max(...gridBlocks.map((b) => b.endMinutes)) : DAY_START_MINUTES

    let currentStart = lastEnd
    const newBlocks = []
    for (const task of toAdd) {
      if (currentStart + 60 > DAY_END_MINUTES) break
      newBlocks.push(makeBlock(task, currentStart, currentStart + 60, gridBlocks.length + newBlocks.length))
      currentStart += 60
    }

    const skipped = toAdd.length - newBlocks.length
    setBlocks([...gridBlocks, ...newBlocks])
    setSelectedTaskIds(new Set())
    if (skipped > 0) {
      setAddWarning(`${skipped} task${skipped > 1 ? 's' : ''} didn't fit — you can resize blocks to make room`)
    }
  }

  function makeBlock(task, startMinutes, endMinutes, sortOrder) {
    return {
      id: `temp-${task.id}-${Date.now()}`,
      blockDate: TODAY,
      startMinutes,
      endMinutes,
      startTime: minutesToTime(startMinutes),
      endTime: minutesToTime(endMinutes),
      sortOrder,
      task,
    }
  }

  // --- Save plan ---
  const saveMutation = useMutation({
    mutationFn: () =>
      savePlan(
        TODAY,
        gridBlocks.map((b) => ({
          taskId: b.task.id,
          startTime: minutesToTime(b.startMinutes),
          endTime: minutesToTime(b.endMinutes),
        }))
      ),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['schedule'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      navigate('/', { state: { successMessage: 'Plan saved. Good luck today!' } })
    },
  })

  // --- dnd-kit drag events ---
  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 5 } }))

  function handleDragStart(event) {
    if (event.active.data.current?.type === 'task-card') {
      setActiveTaskCard(event.active.data.current.task)
    }
  }

  function handleDragEnd(event) {
    setActiveTaskCard(null)
    const { active, over, delta } = event
    if (!over || over.id !== 'time-block-grid') return

    const activeData = active.data.current

    if (activeData.type === 'calendar-block') {
      // Moving an existing block within the calendar
      const { block, blockIndex } = activeData
      const deltaMins = pixelDeltaToMinutes(delta.x)
      const duration = block.endMinutes - block.startMinutes
      const rawStart = block.startMinutes + deltaMins
      const snapped = Math.max(DAY_START_MINUTES, Math.min(DAY_END_MINUTES - duration, snapTo15(rawStart)))

      const updated = gridBlocks.map((b, i) =>
        i === blockIndex ? { ...b, startMinutes: snapped, endMinutes: snapped + duration } : { ...b }
      )
      const sorted = [...updated].sort((a, b) => a.startMinutes - b.startMinutes)
      const movedIndex = sorted.findIndex((b) => b.id === block.id)
      const pushed = pushBlocks(sorted, movedIndex, DAY_END_MINUTES)
      if (pushed) setBlocks(pushed)
    } else if (activeData.type === 'task-card') {
      // Dropping a task card from the browser onto the calendar
      const task = activeData.task
      if (scheduledTaskIds.has(task.id)) return

      // Use the final pointer position over the grid
      const dropClientX = event.activatorEvent.clientX + delta.x
      const rawStart = clientXToMinutes(dropClientX)
      const snapped = Math.max(
        DAY_START_MINUTES,
        Math.min(DAY_END_MINUTES - 60, snapTo15(rawStart))
      )

      const newBlock = makeBlock(task, snapped, snapped + 60, gridBlocks.length)
      const combined = [...gridBlocks, newBlock].sort((a, b) => a.startMinutes - b.startMinutes)
      const insertedIdx = combined.findIndex((b) => b.id === newBlock.id)
      const pushed = pushBlocks(combined, insertedIdx, DAY_END_MINUTES)
      if (pushed) setBlocks(pushed)
    }
  }

  const selectedCount = [...selectedTaskIds].filter((id) => !scheduledTaskIds.has(id)).length

  if (loadingTasks) {
    return <div className="p-8 text-gray-400 text-sm">Loading your tasks…</div>
  }

  return (
    <DndContext sensors={sensors} onDragStart={handleDragStart} onDragEnd={handleDragEnd}>
      <div className="p-6 max-w-7xl mx-auto space-y-4">

        <h1 className="text-2xl font-semibold text-gray-900">
          Start Day — {new Date().toLocaleDateString('en-US', { weekday: 'long', month: 'long', day: 'numeric' })}
        </h1>

        {/* Row 1: All tasks by project */}
        <section className="bg-white border border-gray-200 rounded-lg p-4">
          <div className="text-xs font-semibold text-indigo-700 uppercase tracking-wider mb-3">
            All Tasks
            <span className="ml-2 font-normal text-gray-400 normal-case tracking-normal">
              — drag or check to schedule
            </span>
          </div>
          <TaskBrowserRow
            tasks={suggestedTasks}
            selectedTaskIds={selectedTaskIds}
            scheduledTaskIds={scheduledTaskIds}
            onToggle={toggleTask}
            emptyMessage="No tasks to schedule — you're all caught up!"
          />
          <div className="mt-3 flex items-center gap-3">
            <span className="text-xs text-gray-500">
              {selectedCount > 0 ? `${selectedCount} selected` : 'None selected'}
            </span>
            <button
              onClick={handleAddToCalendar}
              disabled={selectedCount === 0}
              className="px-3 py-1.5 text-xs rounded-md bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-40 transition-colors font-medium"
            >
              + Add to calendar
            </button>
            {addWarning && (
              <span className="text-xs text-amber-600">{addWarning}</span>
            )}
          </div>
        </section>

        {/* Row 2: Due today (left) + Due this week (right) */}
        <div className="flex gap-4">
          <section className="flex-1 min-w-0 bg-white border border-red-200 rounded-lg p-4">
            <div className="text-xs font-semibold text-red-700 uppercase tracking-wider mb-3">
              Due Today
            </div>
            <TaskBrowserRow
              tasks={dueTodayTasks}
              selectedTaskIds={selectedTaskIds}
              scheduledTaskIds={scheduledTaskIds}
              onToggle={toggleTask}
              emptyMessage="Nothing due today"
            />
          </section>

          <section className="flex-1 min-w-0 bg-white border border-amber-200 rounded-lg p-4">
            <div className="text-xs font-semibold text-amber-700 uppercase tracking-wider mb-3">
              Due This Week
            </div>
            <TaskBrowserRow
              tasks={dueThisWeekTasks}
              selectedTaskIds={selectedTaskIds}
              scheduledTaskIds={scheduledTaskIds}
              onToggle={toggleTask}
              emptyMessage="Nothing due this week"
            />
          </section>
        </div>

        {/* Row 3: Horizontal calendar */}
        <section className="bg-white border border-gray-200 rounded-lg p-4">
          <div className="text-xs font-semibold text-indigo-700 uppercase tracking-wider mb-3">
            Today's Plan
            <span className="ml-2 font-normal text-gray-400 normal-case tracking-normal">
              — drag blocks to move · drag right edge to resize
            </span>
          </div>

          <TimeBlockGrid
            blocks={gridBlocks}
            onBlocksChange={setBlocks}
            gridRef={gridRef}
            minutesToPercent={minutesToPercent}
            durationToPercent={durationToPercent}
            startResize={startResize}
          />

          {saveMutation.isError && (
            <p className="mt-2 text-xs text-red-500">
              {saveMutation.error?.message ?? 'Something went wrong. Please try again.'}
            </p>
          )}

          <div className="mt-3 flex justify-end">
            <button
              onClick={() => saveMutation.mutate()}
              disabled={gridBlocks.length === 0 || saveMutation.isPending}
              className="px-4 py-2 text-sm rounded-md bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-40 transition-colors font-medium"
            >
              {saveMutation.isPending ? 'Saving…' : 'Confirm plan'}
            </button>
          </div>
        </section>
      </div>

      {/* DragOverlay — shown while dragging a task card from the browser */}
      <DragOverlay>
        {activeTaskCard ? (
          <div className="bg-indigo-500 text-white text-xs font-medium rounded px-2 py-1.5 shadow-lg opacity-90 max-w-[160px] truncate">
            {activeTaskCard.title}
          </div>
        ) : null}
      </DragOverlay>
    </DndContext>
  )
}
```

- [ ] **Step 2: Start the dev server and verify the page loads without errors**

```bash
cd frontend && npm run dev
```

Open http://localhost:5173/start-day. Expected: page loads, three rows visible, no console errors.

- [ ] **Step 3: Verify lint**

```bash
cd frontend && npm run lint
```

Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/pages/StartDayPage.jsx
git commit -m "feat: implement StartDayPage with three-row layout and drag-and-drop calendar"
```

---

## Task 15: DashboardPage

**Files:**
- Modify: `frontend/src/pages/DashboardPage.jsx`

- [ ] **Step 1: Replace the placeholder with the full implementation**

```jsx
import { useEffect, useState } from 'react'
import { useNavigate, useLocation, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getDashboard } from '@/api/dashboard'

function ProgressBar({ value, max }) {
  const pct = max > 0 ? Math.round((value / max) * 100) : 0
  return (
    <div className="mt-2 h-2 bg-gray-100 rounded-full overflow-hidden">
      <div
        className="h-full bg-indigo-500 rounded-full transition-all"
        style={{ width: `${pct}%` }}
      />
    </div>
  )
}

function Card({ children, className = '' }) {
  return (
    <div className={`bg-white border border-gray-200 rounded-xl p-5 ${className}`}>
      {children}
    </div>
  )
}

function CardLabel({ children }) {
  return (
    <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-2">{children}</p>
  )
}

const DEADLINE_BADGE = {
  TODAY: 'bg-red-100 text-red-700',
  THIS_WEEK: 'bg-amber-100 text-amber-700',
}

export function DashboardPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [toast, setToast] = useState(location.state?.successMessage ?? null)

  useEffect(() => {
    if (toast) {
      window.history.replaceState({}, '')
      const t = setTimeout(() => setToast(null), 3500)
      return () => clearTimeout(t)
    }
  }, [toast])

  const { data, isLoading } = useQuery({
    queryKey: ['dashboard'],
    queryFn: getDashboard,
  })

  if (isLoading) {
    return <div className="p-8 text-gray-400 text-sm">Loading…</div>
  }

  const { todayBlockCount, todayCompletedCount, streakDays, upcomingDeadlines, deferredItemCount } = data ?? {}

  return (
    <div className="p-6 max-w-4xl mx-auto">

      {/* Toast */}
      {toast && (
        <div className="mb-4 px-4 py-3 bg-indigo-50 border border-indigo-200 text-indigo-800 text-sm rounded-lg">
          {toast}
        </div>
      )}

      <h1 className="text-2xl font-semibold text-gray-900 mb-6">Dashboard</h1>

      <div className="grid grid-cols-2 gap-4 mb-6">

        {/* Card 1: Today at a glance */}
        <Card>
          <CardLabel>Today at a Glance</CardLabel>
          {todayBlockCount > 0 ? (
            <>
              <p className="text-2xl font-bold text-gray-900">
                {todayCompletedCount} / {todayBlockCount}
                <span className="text-sm font-normal text-gray-500 ml-2">tasks done</span>
              </p>
              <ProgressBar value={todayCompletedCount} max={todayBlockCount} />
            </>
          ) : (
            <>
              <p className="text-gray-500 text-sm">No plan yet.</p>
              <Link
                to="/start-day"
                className="inline-block mt-2 text-sm text-indigo-600 hover:text-indigo-800 font-medium"
              >
                Start planning →
              </Link>
            </>
          )}
        </Card>

        {/* Card 2: Streak */}
        <Card>
          <CardLabel>Planning Streak</CardLabel>
          {streakDays > 0 ? (
            <>
              <p className="text-2xl font-bold text-gray-900">
                {streakDays}
                <span className="text-sm font-normal text-gray-500 ml-2">
                  {streakDays === 1 ? 'day' : 'days'} in a row
                </span>
              </p>
              <p className="mt-1 text-xs text-gray-400">Keep it going — finish tonight's reflection.</p>
            </>
          ) : (
            <p className="text-sm text-gray-500">Start your streak tonight — finish today's reflection.</p>
          )}
        </Card>

        {/* Card 3: Upcoming deadlines */}
        <Card>
          <CardLabel>Upcoming Deadlines</CardLabel>
          {upcomingDeadlines?.length > 0 ? (
            <ul className="space-y-2">
              {upcomingDeadlines.map((d) => (
                <li
                  key={d.taskId}
                  className="flex items-center gap-2 cursor-pointer hover:bg-gray-50 -mx-1 px-1 rounded"
                  onClick={() => navigate('/start-day')}
                >
                  {d.projectColor && (
                    <span
                      className="w-2.5 h-2.5 rounded-full shrink-0"
                      style={{ background: d.projectColor }}
                    />
                  )}
                  <span className="text-sm text-gray-800 truncate flex-1">{d.taskTitle}</span>
                  {DEADLINE_BADGE[d.deadlineGroup] && (
                    <span className={`text-[10px] font-semibold px-1.5 py-0.5 rounded ${DEADLINE_BADGE[d.deadlineGroup]}`}>
                      {d.deadlineGroup === 'TODAY' ? 'TODAY' : d.projectName}
                    </span>
                  )}
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-sm text-gray-400">No upcoming deadlines. Nice.</p>
          )}
        </Card>

        {/* Card 4: Inbox */}
        <Card
          className={deferredItemCount > 0 ? 'cursor-pointer hover:border-indigo-300 transition-colors' : ''}
          onClick={deferredItemCount > 0 ? () => navigate('/inbox') : undefined}
        >
          <CardLabel>Inbox</CardLabel>
          {deferredItemCount > 0 ? (
            <>
              <p className="text-2xl font-bold text-gray-900">
                {deferredItemCount}
                <span className="text-sm font-normal text-gray-500 ml-2">
                  {deferredItemCount === 1 ? 'item' : 'items'} waiting
                </span>
              </p>
              <p className="mt-1 text-xs text-indigo-500">Click to review →</p>
            </>
          ) : (
            <p className="text-sm text-gray-400">Inbox clear.</p>
          )}
        </Card>
      </div>

      {/* Quick actions */}
      <div className="flex gap-3">
        <Link
          to="/start-day"
          className="px-4 py-2 text-sm rounded-md bg-indigo-600 text-white hover:bg-indigo-700 transition-colors font-medium"
        >
          Start Morning Planning
        </Link>
        <Link
          to="/end-day"
          className="px-4 py-2 text-sm rounded-md border border-gray-300 text-gray-700 hover:bg-gray-50 transition-colors"
        >
          Evening Clean-up
        </Link>
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Start dev server and verify the dashboard loads**

```bash
cd frontend && npm run dev
```

Open http://localhost:5173/. Expected: dashboard page renders with four card placeholders (will show real data once backend is running), no console errors.

- [ ] **Step 3: Verify lint**

```bash
cd frontend && npm run lint
```

Expected: no errors.

- [ ] **Step 4: Run all backend tests one final time to confirm nothing regressed**

```bash
cd backend && mvn test -q
```

Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/pages/DashboardPage.jsx
git commit -m "feat: implement DashboardPage with four-card overview"
```

---

## Final Verification

- [ ] **Start the full stack and do a manual smoke test**

```bash
# Terminal 1
docker compose up -d

# Terminal 2
cd backend && mvn spring-boot:run

# Terminal 3
cd frontend && npm run dev
```

1. Log in at http://localhost:5173/login
2. Navigate to `/` — dashboard loads, cards show (may be empty if no data)
3. Navigate to `/start-day` — three rows visible; task browser shows tasks; calendar grid shows hour lines
4. Check a task and click "+ Add to calendar" — block appears on grid
5. Drag block left/right — snaps to 15-min increments, neighbours push
6. Drag right edge of block — block resizes, neighbours push
7. Drag a task card directly onto the calendar — block appears at drop position
8. Click "Confirm plan" — navigates to `/` with success toast
9. Return to `/start-day` — existing plan pre-populates the calendar

- [ ] **Final commit if any cleanup was done during smoke test**

```bash
git add -p  # stage only intentional changes
git commit -m "fix: smoke test cleanup for Slice 4"
```
