# Slice 3: Evening Clean-up — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver the Evening Clean-up ritual: process deferred inbox items one at a time, complete a daily reflection, and see your streak — plus a functional Inbox page with inline processing actions.

**Architecture:** Backend adds three deferred-item action endpoints (convert/defer/dismiss), a `daily_reflection` package (entity + CRUD + upsert), a stats/streak endpoint, and a completed-today tasks query. Frontend adds shared `ConvertForm`/`DeferredItemActions` components, rebuilds InboxPage, reorganizes the sidebar into navigate/ritual groups, and adds EndDayPage (two-phase wizard).

**Tech Stack:** Java 21, Spring Boot 3, Spring Data JPA, Flyway; React 18, Vite, TanStack Query, Radix UI (Slider, Select), Tailwind CSS

---

## File Map

**Backend — New:**
- `backend/src/main/resources/db/migration/V5__create_daily_reflection.sql`
- `com/planner/backend/deferred/dto/ConvertToTaskRequest.java`
- `com/planner/backend/deferred/dto/DeferRequest.java`
- `com/planner/backend/reflection/DailyReflection.java`
- `com/planner/backend/reflection/DailyReflectionRepository.java`
- `com/planner/backend/reflection/DailyReflectionService.java`
- `com/planner/backend/reflection/DailyReflectionController.java`
- `com/planner/backend/reflection/DailyReflectionExceptionHandler.java`
- `com/planner/backend/reflection/dto/ReflectionRequest.java`
- `com/planner/backend/reflection/dto/ReflectionResponse.java`
- `com/planner/backend/stats/StatsController.java`
- `com/planner/backend/stats/StatsService.java`

**Backend — Modified:**
- `com/planner/backend/deferred/DeferredItemController.java` — add convert, defer, dismiss endpoints
- `com/planner/backend/deferred/DeferredItemService.java` — add convert, defer, dismiss; inject TaskService + TaskRepository
- `com/planner/backend/task/TaskRepository.java` — add `findCompletedTodayForUser`
- `com/planner/backend/task/TaskService.java` — add `listCompletedToday`
- `com/planner/backend/task/TaskController.java` — add `GET /api/v1/tasks/completed-today`

**Backend — Tests:**
- `DeferredItemControllerIntegrationTest.java` — add convert/defer/dismiss test cases
- `backend/src/test/java/com/planner/backend/reflection/DailyReflectionControllerIntegrationTest.java` (new)
- `backend/src/test/java/com/planner/backend/stats/StatsControllerIntegrationTest.java` (new)

**Frontend — New:**
- `frontend/src/api/reflection.js`
- `frontend/src/components/deferred/ConvertForm.jsx`
- `frontend/src/components/deferred/DeferredItemActions.jsx`
- `frontend/src/pages/EndDayPage.jsx`
- `frontend/src/pages/StartDayPage.jsx`

**Frontend — Modified:**
- `frontend/src/api/deferred.js` — add convertDeferredItem, deferDeferredItem, dismissDeferredItem
- `frontend/src/api/tasks.js` — add getTodayCompletedTasks
- `frontend/src/pages/InboxPage.jsx` — full implementation (replaces stub)
- `frontend/src/layouts/AppLayout.jsx` — sidebar groups
- `frontend/src/App.jsx` — add /end-day, /start-day routes

---

## Task 1: Daily Reflection DB Migration

**Files:**
- Create: `backend/src/main/resources/db/migration/V5__create_daily_reflection.sql`

- [ ] **Step 1: Write the migration**

```sql
CREATE TABLE daily_reflection (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES app_user(id),
    reflection_date DATE NOT NULL,
    energy_rating   SMALLINT NOT NULL CHECK (energy_rating BETWEEN 1 AND 5),
    mood_rating     SMALLINT NOT NULL CHECK (mood_rating BETWEEN 1 AND 5),
    reflection_notes TEXT,
    is_finalized    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, reflection_date)
);
```

- [ ] **Step 2: Run Flyway to verify migration applies cleanly**

```bash
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.main.web-application-type=none" -q
```
Expected: Flyway logs `Successfully applied 1 migration to schema "public"` and process exits.

Alternatively, start the full app and check logs:
```bash
docker-compose up -d
cd backend && ./mvnw spring-boot:run
# Look for: Flyway ... Successfully applied 1 migration
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/resources/db/migration/V5__create_daily_reflection.sql
git commit -m "feat: add daily_reflection table migration (Slice 3)"
```

---

## Task 2: Deferred Item Action Endpoints — Backend

**Files:**
- Create: `com/planner/backend/deferred/dto/ConvertToTaskRequest.java`
- Create: `com/planner/backend/deferred/dto/DeferRequest.java`
- Modify: `com/planner/backend/deferred/DeferredItemService.java`
- Modify: `com/planner/backend/deferred/DeferredItemController.java`
- Modify: `backend/src/test/java/com/planner/backend/deferred/DeferredItemControllerIntegrationTest.java`

- [ ] **Step 1: Write the DTOs**

`ConvertToTaskRequest.java`:
```java
package com.planner.backend.deferred.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record ConvertToTaskRequest(
        @NotNull UUID projectId,
        @NotBlank @Size(max = 255) String title,
        String description,
        LocalDate dueDate,
        Short priority,
        Short pointsEstimate
) {}
```

`DeferRequest.java`:
```java
package com.planner.backend.deferred.dto;

import jakarta.validation.constraints.NotNull;

public record DeferRequest(@NotNull DeferDuration deferFor) {
    public enum DeferDuration { ONE_DAY, ONE_WEEK, ONE_MONTH }
}
```

- [ ] **Step 2: Write failing tests for convert, defer, dismiss**

Add to `DeferredItemControllerIntegrationTest.java` (inside the class, after existing tests):

```java
// --- POST /api/v1/deferred/{id}/convert ---

@Test
void convert_validRequest_returns200WithTask() throws Exception {
    UUID itemId = UUID.randomUUID();
    ConvertToTaskRequest req = new ConvertToTaskRequest(
            UUID.randomUUID(), "Buy oat milk", null, null, null, null);

    com.planner.backend.task.dto.TaskResponse taskResp =
            new com.planner.backend.task.dto.TaskResponse(
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    "Buy oat milk", null, null,
                    com.planner.backend.task.TaskStatus.TODO,
                    (short) 3, null, null, null, null, 0,
                    com.planner.backend.task.DeadlineGroup.NO_DEADLINE,
                    java.util.List.of(), null, null, null, null);

    when(deferredItemService.convert(any(AppUser.class), any(UUID.class),
            any(ConvertToTaskRequest.class))).thenReturn(taskResp);

    mockMvc.perform(post("/api/v1/deferred/{id}/convert", itemId)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Buy oat milk"));
}

@Test
void convert_noAuth_returns401() throws Exception {
    mockMvc.perform(post("/api/v1/deferred/{id}/convert", UUID.randomUUID())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"projectId\":\"" + UUID.randomUUID() + "\",\"title\":\"x\"}"))
            .andExpect(status().isUnauthorized());
}

// --- POST /api/v1/deferred/{id}/defer ---

@Test
void defer_validRequest_returns200WithItem() throws Exception {
    UUID itemId = UUID.randomUUID();
    DeferRequest req = new DeferRequest(DeferRequest.DeferDuration.ONE_DAY);
    DeferredItemResponse resp = new DeferredItemResponse(
            itemId, user.getId(), "Buy oat milk",
            false, Instant.now(), null, null, null,
            java.time.LocalDate.now().plusDays(1), 1,
            Instant.now(), Instant.now());

    when(deferredItemService.defer(any(AppUser.class), any(UUID.class),
            any(DeferRequest.class))).thenReturn(resp);

    mockMvc.perform(post("/api/v1/deferred/{id}/defer", itemId)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.deferralCount").value(1));
}

@Test
void defer_noAuth_returns401() throws Exception {
    mockMvc.perform(post("/api/v1/deferred/{id}/defer", UUID.randomUUID())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"deferFor\":\"ONE_DAY\"}"))
            .andExpect(status().isUnauthorized());
}

// --- PATCH /api/v1/deferred/{id}/dismiss ---

@Test
void dismiss_validRequest_returns200() throws Exception {
    UUID itemId = UUID.randomUUID();
    DeferredItemResponse resp = new DeferredItemResponse(
            itemId, user.getId(), "Old thought",
            true, Instant.now(), Instant.now(), null, null, null, 0,
            Instant.now(), Instant.now());

    when(deferredItemService.dismiss(any(AppUser.class), any(UUID.class))).thenReturn(resp);

    mockMvc.perform(patch("/api/v1/deferred/{id}/dismiss", itemId)
                    .header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isProcessed").value(true));
}

@Test
void dismiss_noAuth_returns401() throws Exception {
    mockMvc.perform(patch("/api/v1/deferred/{id}/dismiss", UUID.randomUUID()))
            .andExpect(status().isUnauthorized());
}
```

You also need to add these imports at the top of the test file:
```java
import com.planner.backend.deferred.dto.ConvertToTaskRequest;
import com.planner.backend.deferred.dto.DeferRequest;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
```

- [ ] **Step 3: Run tests to verify they fail**

```bash
cd backend && ./mvnw test -pl . -Dtest=DeferredItemControllerIntegrationTest -q 2>&1 | tail -20
```
Expected: compilation error or test failures referencing missing `convert`, `defer`, `dismiss` methods on `DeferredItemService`.

- [ ] **Step 4: Implement service methods**

Replace `DeferredItemService.java` with:

```java
package com.planner.backend.deferred;

import com.planner.backend.auth.AppUser;
import com.planner.backend.deferred.dto.ConvertToTaskRequest;
import com.planner.backend.deferred.dto.DeferRequest;
import com.planner.backend.deferred.dto.DeferredItemCreateRequest;
import com.planner.backend.deferred.dto.DeferredItemResponse;
import com.planner.backend.task.Task;
import com.planner.backend.task.TaskRepository;
import com.planner.backend.task.TaskService;
import com.planner.backend.task.dto.TaskCreateRequest;
import com.planner.backend.task.dto.TaskResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DeferredItemService {

    private final DeferredItemRepository repository;
    private final TaskService taskService;
    private final TaskRepository taskRepository;

    public DeferredItemService(DeferredItemRepository repository,
                               TaskService taskService,
                               TaskRepository taskRepository) {
        this.repository = repository;
        this.taskService = taskService;
        this.taskRepository = taskRepository;
    }

    public DeferredItemResponse create(AppUser user, DeferredItemCreateRequest request) {
        DeferredItem item = new DeferredItem(user, request.rawText());
        return DeferredItemResponse.from(repository.save(item));
    }

    @Transactional(readOnly = true)
    public List<DeferredItemResponse> listPending(AppUser user) {
        LocalDate today = LocalDate.now(ZoneId.of(user.getTimezone()));
        return repository.findPendingForUser(user, today).stream()
                .map(DeferredItemResponse::from)
                .toList();
    }

    public TaskResponse convert(AppUser user, UUID itemId, ConvertToTaskRequest request) {
        DeferredItem item = findOwnedItem(user, itemId);
        TaskCreateRequest taskReq = new TaskCreateRequest(
                request.title(), request.description(), null, null,
                request.priority(), request.pointsEstimate(), null, null,
                request.dueDate(), null);
        TaskResponse created = taskService.create(user, request.projectId(), taskReq);
        Task task = taskRepository.findById(created.id())
                .orElseThrow(() -> new DeferredItemNotFoundException("Task not found after creation"));
        item.setProcessed(true);
        item.setProcessedAt(Instant.now());
        item.setResolvedTask(task);
        return created;
    }

    public DeferredItemResponse defer(AppUser user, UUID itemId, DeferRequest request) {
        DeferredItem item = findOwnedItem(user, itemId);
        LocalDate until = switch (request.deferFor()) {
            case ONE_DAY -> LocalDate.now(ZoneId.of(user.getTimezone())).plusDays(1);
            case ONE_WEEK -> LocalDate.now(ZoneId.of(user.getTimezone())).plusWeeks(1);
            case ONE_MONTH -> LocalDate.now(ZoneId.of(user.getTimezone())).plusMonths(1);
        };
        item.setDeferredUntilDate(until);
        item.setDeferralCount(item.getDeferralCount() + 1);
        return DeferredItemResponse.from(item);
    }

    public DeferredItemResponse dismiss(AppUser user, UUID itemId) {
        DeferredItem item = findOwnedItem(user, itemId);
        item.setProcessed(true);
        item.setProcessedAt(Instant.now());
        return DeferredItemResponse.from(item);
    }

    private DeferredItem findOwnedItem(AppUser user, UUID itemId) {
        return repository.findByIdAndUserId(user, itemId)
                .orElseThrow(() -> new DeferredItemNotFoundException("Deferred item not found: " + itemId));
    }
}
```

- [ ] **Step 5: Add `findByIdAndUserId` to `DeferredItemRepository`**

Add to `DeferredItemRepository.java`:
```java
import java.util.Optional;

@Query("SELECT d FROM DeferredItem d WHERE d.id = :id AND d.user = :user")
Optional<DeferredItem> findByIdAndUserId(@Param("user") AppUser user, @Param("id") UUID id);
```

- [ ] **Step 6: Add endpoints to `DeferredItemController`**

Add these three methods to `DeferredItemController.java` (after `listPending`):

```java
@PostMapping("/api/v1/deferred/{id}/convert")
public ResponseEntity<TaskResponse> convert(
        @AuthenticationPrincipal AppUser user,
        @PathVariable UUID id,
        @Valid @RequestBody ConvertToTaskRequest request) {
    return ResponseEntity.ok(service.convert(user, id, request));
}

@PostMapping("/api/v1/deferred/{id}/defer")
public ResponseEntity<DeferredItemResponse> defer(
        @AuthenticationPrincipal AppUser user,
        @PathVariable UUID id,
        @Valid @RequestBody DeferRequest request) {
    return ResponseEntity.ok(service.defer(user, id, request));
}

@PatchMapping("/api/v1/deferred/{id}/dismiss")
public ResponseEntity<DeferredItemResponse> dismiss(
        @AuthenticationPrincipal AppUser user,
        @PathVariable UUID id) {
    return ResponseEntity.ok(service.dismiss(user, id));
}
```

Add these imports to `DeferredItemController.java`:
```java
import com.planner.backend.deferred.dto.ConvertToTaskRequest;
import com.planner.backend.deferred.dto.DeferRequest;
import com.planner.backend.task.dto.TaskResponse;
```

- [ ] **Step 7: Add `DeferredItemNotFoundException` to the existing exception handler**

Open `DeferredItemExceptionHandler.java` and check if it handles `DeferredItemNotFoundException`. If the class doesn't exist yet, create it in the `deferred` package:

```java
package com.planner.backend.deferred;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = DeferredItemController.class)
public class DeferredItemExceptionHandler {

    @ExceptionHandler(DeferredItemNotFoundException.class)
    public ResponseEntity<String> handleNotFound(DeferredItemNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}
```

Then create `DeferredItemNotFoundException.java` in the `deferred` package:
```java
package com.planner.backend.deferred;

public class DeferredItemNotFoundException extends RuntimeException {
    public DeferredItemNotFoundException(String message) {
        super(message);
    }
}
```

Also add the `@Import(DeferredItemExceptionHandler.class)` to the test if it's not already there (check the existing `@Import` annotation in the test class — it already includes it).

- [ ] **Step 8: Add `TaskResponse` fields**

Check what fields `TaskResponse` has by reading `com/planner/backend/task/dto/TaskResponse.java`. The test above constructs it directly, so ensure the constructor args match the record definition. Adjust the test's `TaskResponse` constructor call to match the actual record fields.

- [ ] **Step 9: Run tests to verify they pass**

```bash
cd backend && ./mvnw test -pl . -Dtest=DeferredItemControllerIntegrationTest -q 2>&1 | tail -20
```
Expected: `Tests run: N, Failures: 0, Errors: 0`

- [ ] **Step 10: Commit**

```bash
git add backend/src/main/java/com/planner/backend/deferred/ \
        backend/src/test/java/com/planner/backend/deferred/
git commit -m "feat: add deferred item convert/defer/dismiss endpoints (Slice 3)"
```

---

## Task 3: Daily Reflection — Backend

**Files:**
- Create: `com/planner/backend/reflection/DailyReflection.java`
- Create: `com/planner/backend/reflection/DailyReflectionRepository.java`
- Create: `com/planner/backend/reflection/DailyReflectionService.java`
- Create: `com/planner/backend/reflection/DailyReflectionController.java`
- Create: `com/planner/backend/reflection/DailyReflectionExceptionHandler.java`
- Create: `com/planner/backend/reflection/dto/ReflectionRequest.java`
- Create: `com/planner/backend/reflection/dto/ReflectionResponse.java`
- Create: `backend/src/test/java/com/planner/backend/reflection/DailyReflectionControllerIntegrationTest.java`

- [ ] **Step 1: Write failing tests**

Create `DailyReflectionControllerIntegrationTest.java`:

```java
package com.planner.backend.reflection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planner.backend.auth.AppUser;
import com.planner.backend.auth.AppUserRepository;
import com.planner.backend.auth.JwtAuthFilter;
import com.planner.backend.auth.JwtService;
import com.planner.backend.auth.SecurityConfig;
import com.planner.backend.reflection.dto.ReflectionRequest;
import com.planner.backend.reflection.dto.ReflectionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DailyReflectionController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthFilter.class, DailyReflectionExceptionHandler.class})
class DailyReflectionControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JwtService jwtService;

    @MockBean AppUserRepository userRepository;
    @MockBean DailyReflectionService reflectionService;

    private AppUser user;
    private String accessToken;

    @BeforeEach
    void setUp() {
        user = new AppUser("alice@example.com", "hash", "Alice", "UTC");
        accessToken = jwtService.generateAccessToken("alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
    }

    @Test
    void reflect_validRequest_returns200() throws Exception {
        ReflectionRequest req = new ReflectionRequest((short) 4, (short) 3, "Good day", true);
        ReflectionResponse resp = new ReflectionResponse(
                UUID.randomUUID(), user.getId(), LocalDate.now(),
                (short) 4, (short) 3, "Good day", true,
                Instant.now(), Instant.now());
        when(reflectionService.upsert(any(AppUser.class), any(ReflectionRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/schedule/today/reflect")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.energyRating").value(4))
                .andExpect(jsonPath("$.isFinalized").value(true));
    }

    @Test
    void reflect_invalidRating_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/schedule/today/reflect")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"energyRating\":6,\"moodRating\":3,\"isFinalized\":false}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reflect_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/schedule/today/reflect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"energyRating\":3,\"moodRating\":3,\"isFinalized\":false}"))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd backend && ./mvnw test -pl . -Dtest=DailyReflectionControllerIntegrationTest -q 2>&1 | tail -10
```
Expected: compilation errors (classes don't exist yet).

- [ ] **Step 3: Create `DailyReflection` entity**

```java
package com.planner.backend.reflection;

import com.planner.backend.auth.AppUser;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_reflection")
public class DailyReflection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "reflection_date", nullable = false)
    private LocalDate reflectionDate;

    @Column(name = "energy_rating", nullable = false)
    private short energyRating;

    @Column(name = "mood_rating", nullable = false)
    private short moodRating;

    @Column(name = "reflection_notes", columnDefinition = "TEXT")
    private String reflectionNotes;

    @Column(name = "is_finalized", nullable = false)
    private boolean isFinalized = false;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    public DailyReflection() {}

    public DailyReflection(AppUser user, LocalDate reflectionDate) {
        this.user = user;
        this.reflectionDate = reflectionDate;
    }

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public LocalDate getReflectionDate() { return reflectionDate; }
    public short getEnergyRating() { return energyRating; }
    public short getMoodRating() { return moodRating; }
    public String getReflectionNotes() { return reflectionNotes; }
    public boolean isFinalized() { return isFinalized; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setEnergyRating(short energyRating) { this.energyRating = energyRating; }
    public void setMoodRating(short moodRating) { this.moodRating = moodRating; }
    public void setReflectionNotes(String reflectionNotes) { this.reflectionNotes = reflectionNotes; }
    public void setFinalized(boolean finalized) { isFinalized = finalized; }
}
```

- [ ] **Step 4: Create `DailyReflectionRepository`**

```java
package com.planner.backend.reflection;

import com.planner.backend.auth.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyReflectionRepository extends JpaRepository<DailyReflection, UUID> {

    Optional<DailyReflection> findByUserAndReflectionDate(AppUser user, LocalDate date);

    @Query("""
            SELECT r.reflectionDate FROM DailyReflection r
            WHERE r.user = :user AND r.isFinalized = true
            ORDER BY r.reflectionDate DESC
            """)
    List<LocalDate> findFinalizedDatesDesc(@Param("user") AppUser user);
}
```

- [ ] **Step 5: Create DTOs**

`ReflectionRequest.java`:
```java
package com.planner.backend.reflection.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReflectionRequest(
        @NotNull @Min(1) @Max(5) Short energyRating,
        @NotNull @Min(1) @Max(5) Short moodRating,
        String reflectionNotes,
        boolean isFinalized
) {}
```

`ReflectionResponse.java`:
```java
package com.planner.backend.reflection.dto;

import com.planner.backend.reflection.DailyReflection;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReflectionResponse(
        UUID id,
        UUID userId,
        LocalDate reflectionDate,
        short energyRating,
        short moodRating,
        String reflectionNotes,
        boolean isFinalized,
        Instant createdAt,
        Instant updatedAt
) {
    public static ReflectionResponse from(DailyReflection r) {
        return new ReflectionResponse(
                r.getId(), r.getUser().getId(), r.getReflectionDate(),
                r.getEnergyRating(), r.getMoodRating(), r.getReflectionNotes(),
                r.isFinalized(), r.getCreatedAt(), r.getUpdatedAt());
    }
}
```

- [ ] **Step 6: Create `DailyReflectionService`**

```java
package com.planner.backend.reflection;

import com.planner.backend.auth.AppUser;
import com.planner.backend.reflection.dto.ReflectionRequest;
import com.planner.backend.reflection.dto.ReflectionResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@Transactional
public class DailyReflectionService {

    private final DailyReflectionRepository repository;

    public DailyReflectionService(DailyReflectionRepository repository) {
        this.repository = repository;
    }

    public ReflectionResponse upsert(AppUser user, ReflectionRequest request) {
        LocalDate today = LocalDate.now(ZoneId.of(user.getTimezone()));
        DailyReflection reflection = repository
                .findByUserAndReflectionDate(user, today)
                .orElseGet(() -> new DailyReflection(user, today));
        reflection.setEnergyRating(request.energyRating());
        reflection.setMoodRating(request.moodRating());
        reflection.setReflectionNotes(request.reflectionNotes());
        reflection.setFinalized(request.isFinalized());
        return ReflectionResponse.from(repository.save(reflection));
    }
}
```

- [ ] **Step 7: Create `DailyReflectionController` and `DailyReflectionExceptionHandler`**

`DailyReflectionController.java`:
```java
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
```

`DailyReflectionExceptionHandler.java`:
```java
package com.planner.backend.reflection;

import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = DailyReflectionController.class)
public class DailyReflectionExceptionHandler {
    // validation errors are handled globally by Spring's default error handling
}
```

- [ ] **Step 8: Run tests to verify they pass**

```bash
cd backend && ./mvnw test -pl . -Dtest=DailyReflectionControllerIntegrationTest -q 2>&1 | tail -10
```
Expected: `Tests run: 3, Failures: 0, Errors: 0`

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/com/planner/backend/reflection/ \
        backend/src/test/java/com/planner/backend/reflection/
git commit -m "feat: add daily reflection upsert endpoint (Slice 3)"
```

---

## Task 4: Streak Endpoint — Backend

**Files:**
- Create: `com/planner/backend/stats/StatsService.java`
- Create: `com/planner/backend/stats/StatsController.java`
- Create: `backend/src/test/java/com/planner/backend/stats/StatsControllerIntegrationTest.java`

- [ ] **Step 1: Write failing tests**

Create `StatsControllerIntegrationTest.java`:

```java
package com.planner.backend.stats;

import com.planner.backend.auth.AppUser;
import com.planner.backend.auth.AppUserRepository;
import com.planner.backend.auth.JwtAuthFilter;
import com.planner.backend.auth.JwtService;
import com.planner.backend.auth.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatsController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthFilter.class})
class StatsControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtService jwtService;

    @MockBean AppUserRepository userRepository;
    @MockBean StatsService statsService;

    private AppUser user;
    private String accessToken;

    @BeforeEach
    void setUp() {
        user = new AppUser("alice@example.com", "hash", "Alice", "UTC");
        accessToken = jwtService.generateAccessToken("alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
    }

    @Test
    void streak_returnsCount() throws Exception {
        when(statsService.getStreak(any(AppUser.class))).thenReturn(5);

        mockMvc.perform(get("/api/v1/stats/streak")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.streak").value(5));
    }

    @Test
    void streak_noStreak_returnsZero() throws Exception {
        when(statsService.getStreak(any(AppUser.class))).thenReturn(0);

        mockMvc.perform(get("/api/v1/stats/streak")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.streak").value(0));
    }

    @Test
    void streak_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/stats/streak"))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
cd backend && ./mvnw test -pl . -Dtest=StatsControllerIntegrationTest -q 2>&1 | tail -10
```
Expected: compilation error (classes don't exist).

- [ ] **Step 3: Create `StatsService`**

```java
package com.planner.backend.stats;

import com.planner.backend.auth.AppUser;
import com.planner.backend.reflection.DailyReflectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class StatsService {

    private final DailyReflectionRepository reflectionRepository;

    public StatsService(DailyReflectionRepository reflectionRepository) {
        this.reflectionRepository = reflectionRepository;
    }

    public int getStreak(AppUser user) {
        List<LocalDate> dates = reflectionRepository.findFinalizedDatesDesc(user);
        LocalDate expected = LocalDate.now(ZoneId.of(user.getTimezone()));
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
}
```

- [ ] **Step 4: Create `StatsController`**

```java
package com.planner.backend.stats;

import com.planner.backend.auth.AppUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/api/v1/stats/streak")
    public ResponseEntity<Map<String, Integer>> streak(@AuthenticationPrincipal AppUser user) {
        return ResponseEntity.ok(Map.of("streak", statsService.getStreak(user)));
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

```bash
cd backend && ./mvnw test -pl . -Dtest=StatsControllerIntegrationTest -q 2>&1 | tail -10
```
Expected: `Tests run: 3, Failures: 0, Errors: 0`

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/planner/backend/stats/ \
        backend/src/test/java/com/planner/backend/stats/
git commit -m "feat: add streak stats endpoint (Slice 3)"
```

---

## Task 5: Completed-Today Tasks Endpoint — Backend

**Files:**
- Modify: `com/planner/backend/task/TaskRepository.java`
- Modify: `com/planner/backend/task/TaskService.java`
- Modify: `com/planner/backend/task/TaskController.java`

No new test file needed — add cases to existing task controller test, or test manually. The endpoint is simple enough that a brief manual test via curl suffices; add a focused test below.

- [ ] **Step 1: Add repository query to `TaskRepository`**

Add to `TaskRepository.java`:
```java
import java.time.Instant;

@Query("""
        SELECT t FROM Task t
        WHERE t.user.id = :userId
          AND t.status = com.planner.backend.task.TaskStatus.DONE
          AND t.completedAt >= :startOfDay
          AND t.archivedAt IS NULL
        ORDER BY t.completedAt DESC
        """)
List<Task> findCompletedTodayForUser(@Param("userId") UUID userId,
                                     @Param("startOfDay") Instant startOfDay);
```

Add this import to `TaskRepository.java`:
```java
import org.springframework.data.jpa.repository.Param;
import java.time.Instant;
```

- [ ] **Step 2: Add service method to `TaskService`**

Add to `TaskService.java`:
```java
@Transactional(readOnly = true)
public List<TaskResponse> listCompletedToday(AppUser user) {
    Instant startOfDay = LocalDate.now(java.time.ZoneOffset.UTC)
            .atStartOfDay(java.time.ZoneOffset.UTC)
            .toInstant();
    List<Task> tasks = taskRepository.findCompletedTodayForUser(user.getId(), startOfDay);
    return tasks.stream()
            .map(t -> TaskResponse.from(t, computeDeadlineGroup(t.getDueDate()), List.of()))
            .toList();
}
```

- [ ] **Step 3: Add endpoint to `TaskController`**

Add to `TaskController.java`:
```java
@GetMapping("/api/v1/tasks/completed-today")
public ResponseEntity<List<TaskResponse>> listCompletedToday(
        @AuthenticationPrincipal AppUser user) {
    return ResponseEntity.ok(taskService.listCompletedToday(user));
}
```

- [ ] **Step 4: Run all backend tests to check nothing broke**

```bash
cd backend && ./mvnw test -q 2>&1 | tail -15
```
Expected: `BUILD SUCCESS`, no failures.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/planner/backend/task/
git commit -m "feat: add completed-today tasks endpoint (Slice 3)"
```

---

## Task 6: Frontend API Layer

**Files:**
- Modify: `frontend/src/api/deferred.js`
- Modify: `frontend/src/api/tasks.js`
- Create: `frontend/src/api/reflection.js`

- [ ] **Step 1: Add deferred action functions to `deferred.js`**

Add to `frontend/src/api/deferred.js`:
```js
export async function convertDeferredItem(id, payload) {
  const res = await authFetch(`${BASE}/deferred/${id}/convert`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
  return handleResponse(res)
}

export async function deferDeferredItem(id, deferFor) {
  const res = await authFetch(`${BASE}/deferred/${id}/defer`, {
    method: 'POST',
    body: JSON.stringify({ deferFor }),
  })
  return handleResponse(res)
}

export async function dismissDeferredItem(id) {
  const res = await authFetch(`${BASE}/deferred/${id}/dismiss`, {
    method: 'PATCH',
  })
  return handleResponse(res)
}
```

- [ ] **Step 2: Add completed-today function to `tasks.js`**

Add to `frontend/src/api/tasks.js`:
```js
export async function getTodayCompletedTasks() {
  const res = await authFetch(`/api/v1/tasks/completed-today`)
  return handleResponse(res)
}
```

- [ ] **Step 3: Create `reflection.js`**

```js
import { authFetch, handleResponse } from './client'

const BASE = '/api/v1'

export async function saveReflection(payload) {
  const res = await authFetch(`${BASE}/schedule/today/reflect`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
  return handleResponse(res)
}

export async function getStreak() {
  const res = await authFetch(`${BASE}/stats/streak`)
  return handleResponse(res)
}
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/api/
git commit -m "feat: add frontend API functions for Slice 3 endpoints"
```

---

## Task 7: Shared Deferred Components

**Files:**
- Create: `frontend/src/components/deferred/ConvertForm.jsx`
- Create: `frontend/src/components/deferred/DeferredItemActions.jsx`

These components are used by both `InboxPage` and `EndDayPage`.

- [ ] **Step 1: Create `ConvertForm.jsx`**

```jsx
import { useState } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { getProjects } from '@/api/projects'
import { convertDeferredItem } from '@/api/deferred'

export function ConvertForm({ item, onDone, onCancel }) {
  const [title, setTitle] = useState(item.rawText)
  const [projectId, setProjectId] = useState('')
  const [description, setDescription] = useState('')
  const [dueDate, setDueDate] = useState('')
  const [priority, setPriority] = useState('')
  const [points, setPoints] = useState('')

  const { data: projects = [] } = useQuery({
    queryKey: ['projects'],
    queryFn: getProjects,
  })

  const mutation = useMutation({
    mutationFn: () =>
      convertDeferredItem(item.id, {
        projectId,
        title,
        description: description || null,
        dueDate: dueDate || null,
        priority: priority ? Number(priority) : null,
        pointsEstimate: points ? Number(points) : null,
      }),
    onSuccess: (task) => onDone(task),
  })

  return (
    <form
      className="mt-3 space-y-3 border-t border-gray-100 pt-3"
      onSubmit={(e) => { e.preventDefault(); mutation.mutate() }}
    >
      <div>
        <label className="block text-xs font-medium text-gray-600 mb-1">Project *</label>
        <select
          className="w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          value={projectId}
          onChange={(e) => setProjectId(e.target.value)}
          required
        >
          <option value="">Select a project…</option>
          {projects.map((p) => (
            <option key={p.id} value={p.id}>{p.name}</option>
          ))}
        </select>
      </div>

      <div>
        <label className="block text-xs font-medium text-gray-600 mb-1">Title *</label>
        <input
          className="w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          required
        />
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className="block text-xs font-medium text-gray-600 mb-1">Due date</label>
          <input
            type="date"
            className="w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            value={dueDate}
            onChange={(e) => setDueDate(e.target.value)}
          />
        </div>
        <div>
          <label className="block text-xs font-medium text-gray-600 mb-1">Priority (1–5)</label>
          <input
            type="number" min="1" max="5"
            className="w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            value={priority}
            onChange={(e) => setPriority(e.target.value)}
          />
        </div>
      </div>

      <div>
        <label className="block text-xs font-medium text-gray-600 mb-1">Points estimate</label>
        <input
          type="number" min="1"
          className="w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          value={points}
          onChange={(e) => setPoints(e.target.value)}
        />
      </div>

      <div>
        <label className="block text-xs font-medium text-gray-600 mb-1">Description</label>
        <textarea
          rows={2}
          className="w-full rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 resize-none"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
      </div>

      {mutation.isError && (
        <p className="text-xs text-red-500">Something went wrong. Try again.</p>
      )}

      <div className="flex gap-2 justify-end">
        <button
          type="button"
          onClick={onCancel}
          className="px-3 py-1.5 text-sm rounded-md text-gray-600 hover:bg-gray-100 transition-colors"
        >
          Cancel
        </button>
        <button
          type="submit"
          disabled={mutation.isPending}
          className="px-3 py-1.5 text-sm rounded-md bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-50 transition-colors"
        >
          {mutation.isPending ? 'Creating…' : 'Create task'}
        </button>
      </div>
    </form>
  )
}
```

- [ ] **Step 2: Create `DeferredItemActions.jsx`**

```jsx
import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { deferDeferredItem, dismissDeferredItem } from '@/api/deferred'
import { ConvertForm } from './ConvertForm'
import { formatDistanceToNow } from 'date-fns'

export function DeferredItemActions({ item, onDone }) {
  const [mode, setMode] = useState(null) // null | 'convert' | 'defer' | 'dismiss-confirm'

  const deferMutation = useMutation({
    mutationFn: (deferFor) => deferDeferredItem(item.id, deferFor),
    onSuccess: () => onDone(),
  })

  const dismissMutation = useMutation({
    mutationFn: () => dismissDeferredItem(item.id),
    onSuccess: () => onDone(),
  })

  const capturedAgo = formatDistanceToNow(new Date(item.capturedAt), { addSuffix: true })

  return (
    <div>
      <p className="text-sm text-gray-500 mb-1">{capturedAgo}</p>
      <p className="text-gray-900 mb-3">{item.rawText}</p>

      {mode === null && (
        <div className="flex gap-2">
          <button
            onClick={() => setMode('convert')}
            className="px-3 py-1.5 text-sm rounded-md bg-indigo-600 text-white hover:bg-indigo-700 transition-colors"
          >
            Convert
          </button>
          <button
            onClick={() => setMode('defer')}
            className="px-3 py-1.5 text-sm rounded-md border border-gray-300 text-gray-700 hover:bg-gray-50 transition-colors"
          >
            Defer
          </button>
          <button
            onClick={() => setMode('dismiss-confirm')}
            className="px-3 py-1.5 text-sm rounded-md border border-gray-300 text-gray-500 hover:bg-gray-50 transition-colors"
          >
            Dismiss
          </button>
        </div>
      )}

      {mode === 'convert' && (
        <ConvertForm item={item} onDone={() => onDone()} onCancel={() => setMode(null)} />
      )}

      {mode === 'defer' && (
        <div className="flex gap-2 mt-1">
          <span className="text-sm text-gray-500 self-center mr-1">Defer for:</span>
          {[
            { label: '1 day', value: 'ONE_DAY' },
            { label: '1 week', value: 'ONE_WEEK' },
            { label: '1 month', value: 'ONE_MONTH' },
          ].map(({ label, value }) => (
            <button
              key={value}
              onClick={() => deferMutation.mutate(value)}
              disabled={deferMutation.isPending}
              className="px-3 py-1.5 text-sm rounded-md border border-gray-300 text-gray-700 hover:bg-gray-50 disabled:opacity-50 transition-colors"
            >
              {label}
            </button>
          ))}
          <button
            onClick={() => setMode(null)}
            className="px-2 py-1.5 text-sm text-gray-400 hover:text-gray-600"
          >
            ✕
          </button>
        </div>
      )}

      {mode === 'dismiss-confirm' && (
        <div className="flex gap-2 mt-1 items-center">
          <span className="text-sm text-gray-500">Sure?</span>
          <button
            onClick={() => dismissMutation.mutate()}
            disabled={dismissMutation.isPending}
            className="px-3 py-1.5 text-sm rounded-md bg-red-50 text-red-700 border border-red-200 hover:bg-red-100 disabled:opacity-50 transition-colors"
          >
            Yes, dismiss
          </button>
          <button
            onClick={() => setMode(null)}
            className="px-2 py-1.5 text-sm text-gray-400 hover:text-gray-600"
          >
            Cancel
          </button>
        </div>
      )}
    </div>
  )
}
```

- [ ] **Step 3: Install `date-fns` if not already present**

```bash
cd frontend && npm list date-fns 2>/dev/null | grep date-fns || npm install date-fns
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/components/deferred/
git commit -m "feat: add ConvertForm and DeferredItemActions shared components (Slice 3)"
```

---

## Task 8: Inbox Page

**Files:**
- Modify: `frontend/src/pages/InboxPage.jsx`

- [ ] **Step 1: Rewrite `InboxPage.jsx`**

```jsx
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { getDeferredItems } from '@/api/deferred'
import { DeferredItemActions } from '@/components/deferred/DeferredItemActions'

export function InboxPage() {
  const queryClient = useQueryClient()
  const { data: items = [], isLoading } = useQuery({
    queryKey: ['deferred'],
    queryFn: getDeferredItems,
  })

  function handleDone() {
    queryClient.invalidateQueries({ queryKey: ['deferred'] })
  }

  if (isLoading) {
    return <div className="p-8 text-gray-400">Loading…</div>
  }

  return (
    <div className="p-8 max-w-2xl">
      <h1 className="text-2xl font-semibold text-gray-900 mb-6">Inbox</h1>

      {items.length === 0 ? (
        <p className="text-gray-400">Nothing to process.</p>
      ) : (
        <ul className="space-y-4">
          {items.map((item) => (
            <li key={item.id} className="bg-white border border-gray-200 rounded-lg p-4">
              <DeferredItemActions item={item} onDone={handleDone} />
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/pages/InboxPage.jsx
git commit -m "feat: implement Inbox page with inline deferred item actions (Slice 3)"
```

---

## Task 9: Sidebar Reorganization

**Files:**
- Modify: `frontend/src/layouts/AppLayout.jsx`

- [ ] **Step 1: Update `AppLayout.jsx`**

Replace the `NAV_ITEMS` array and the nav section in `AppLayout.jsx`. The sidebar now renders two groups with a divider between them.

Replace from `const NAV_ITEMS = [` through the closing `]` and the `NavItem` component and the sidebar `div` that renders nav items, with:

```jsx
const NAVIGATE_ITEMS = [
  {
    label: 'Dashboard',
    to: '/',
    end: true,
    icon: (
      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none"
        stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <rect x="3" y="3" width="7" height="7" rx="1" />
        <rect x="14" y="3" width="7" height="7" rx="1" />
        <rect x="3" y="14" width="7" height="7" rx="1" />
        <rect x="14" y="14" width="7" height="7" rx="1" />
      </svg>
    ),
  },
  {
    label: 'Today',
    to: '/today',
    icon: (
      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none"
        stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <circle cx="12" cy="12" r="4" />
        <line x1="12" y1="2" x2="12" y2="4" />
        <line x1="12" y1="20" x2="12" y2="22" />
        <line x1="2" y1="12" x2="4" y2="12" />
        <line x1="20" y1="12" x2="22" y2="12" />
        <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" />
        <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" />
        <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" />
        <line x1="18.36" y1="5.64" x2="19.78" y2="4.22" />
      </svg>
    ),
  },
  {
    label: 'Projects',
    to: '/projects',
    icon: (
      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none"
        stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
      </svg>
    ),
  },
  {
    label: 'Inbox',
    to: '/inbox',
    icon: (
      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none"
        stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <polyline points="22 12 16 12 14 15 10 15 8 12 2 12" />
        <path d="M5.45 5.11L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z" />
      </svg>
    ),
  },
]

const RITUAL_ITEMS = [
  {
    label: 'Start Day',
    to: '/start-day',
    icon: (
      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none"
        stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
      </svg>
    ),
  },
  {
    label: 'End Day',
    to: '/end-day',
    icon: (
      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none"
        stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
      </svg>
    ),
  },
]
```

Then replace the nav links section (inside the sidebar `<div className="flex-1 overflow-y-auto...">`) with:

```jsx
<div className="flex-1 overflow-y-auto px-3 py-4 space-y-1">
  {NAVIGATE_ITEMS.map((item) => (
    <NavItem
      key={item.to}
      item={item}
      badge={item.to === '/inbox' ? inboxCount : 0}
    />
  ))}

  <div className="pt-3 pb-1">
    <p className="px-3 text-xs font-medium text-gray-400 uppercase tracking-wider">
      Daily rituals
    </p>
  </div>

  {RITUAL_ITEMS.map((item) => (
    <NavItem key={item.to} item={item} ritual />
  ))}
</div>
```

Also update `NavItem` to accept and apply the `ritual` prop:

```jsx
function NavItem({ item, badge = 0, ritual = false }) {
  return (
    <NavLink
      to={item.to}
      end={item.end}
      className={({ isActive }) =>
        [
          'flex items-center gap-3 px-3 py-2 rounded-md text-sm font-medium transition-colors duration-100 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-1',
          isActive
            ? 'bg-indigo-50 text-indigo-700'
            : ritual
            ? 'text-indigo-600 hover:bg-indigo-50 hover:text-indigo-700'
            : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900',
        ].join(' ')
      }
    >
      {item.icon}
      {item.label}
      {badge > 0 && (
        <span className="ml-auto bg-indigo-100 text-indigo-700 text-xs font-semibold rounded-full px-1.5 py-0.5 min-w-[1.25rem] text-center">
          {badge > 99 ? '99+' : badge}
        </span>
      )}
    </NavLink>
  )
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/layouts/AppLayout.jsx
git commit -m "feat: reorganize sidebar into navigate/rituals groups (Slice 3)"
```

---

## Task 10: End Day Page — Phase 1 (Deferred Items Flow)

**Files:**
- Create: `frontend/src/pages/EndDayPage.jsx` (Phase 1 only — Phase 2 added in Task 11)

- [ ] **Step 1: Create `EndDayPage.jsx` with Phase 1**

```jsx
import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { getDeferredItems } from '@/api/deferred'
import { DeferredItemActions } from '@/components/deferred/DeferredItemActions'

// Phase2 will be added in the next task
function Phase2() {
  return <div className="text-gray-400">Reflection coming soon…</div>
}

export function EndDayPage() {
  const queryClient = useQueryClient()
  const { data: items = [], isLoading } = useQuery({
    queryKey: ['deferred'],
    queryFn: getDeferredItems,
  })

  const [currentIndex, setCurrentIndex] = useState(0)
  const [phase, setPhase] = useState(1) // 1 or 2
  const [showCelebration, setShowCelebration] = useState(false)

  function handleItemDone() {
    queryClient.invalidateQueries({ queryKey: ['deferred'] })
    const remaining = items.length - (currentIndex + 1)
    if (remaining <= 0) {
      setShowCelebration(true)
      setTimeout(() => {
        setShowCelebration(false)
        setPhase(2)
      }, 2000)
    } else {
      setCurrentIndex((i) => i + 1)
    }
  }

  if (isLoading) {
    return <div className="p-8 text-gray-400">Loading…</div>
  }

  if (phase === 2) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-start justify-center pt-16 px-4">
        <div className="w-full max-w-lg">
          <Phase2 />
        </div>
      </div>
    )
  }

  const noItems = items.length === 0
  const currentItem = items[currentIndex]

  if (showCelebration) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="text-center">
          <div className="text-5xl mb-4">🎉</div>
          <h2 className="text-2xl font-semibold text-gray-900">Inbox Zero!</h2>
          <p className="mt-2 text-gray-500">All caught up.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gray-50 flex items-start justify-center pt-16 px-4">
      <div className="w-full max-w-lg">
        <h1 className="text-2xl font-semibold text-gray-900 mb-2">End of Day</h1>

        {noItems ? (
          <div className="mb-8">
            <p className="text-gray-500 text-sm mb-6">Inbox is clear.</p>
          </div>
        ) : (
          <div className="mb-8">
            <p className="text-sm text-gray-400 mb-4">
              {currentIndex + 1} of {items.length}
            </p>
            <div className="bg-white border border-gray-200 rounded-lg p-6 shadow-sm">
              {currentItem && (
                <DeferredItemActions item={currentItem} onDone={handleItemDone} />
              )}
            </div>
          </div>
        )}

        {noItems && (
          <button
            onClick={() => setPhase(2)}
            className="px-4 py-2 text-sm rounded-md bg-indigo-600 text-white hover:bg-indigo-700 transition-colors"
          >
            Continue to reflection →
          </button>
        )}
      </div>
    </div>
  )
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/pages/EndDayPage.jsx
git commit -m "feat: add End Day page Phase 1 — deferred items flow (Slice 3)"
```

---

## Task 11: End Day Page — Phase 2 (Reflection + Streak)

**Files:**
- Modify: `frontend/src/pages/EndDayPage.jsx` (replace Phase2 stub with real implementation)

- [ ] **Step 1: Replace the `Phase2` stub in `EndDayPage.jsx`**

Add these imports at the top of `EndDayPage.jsx`:
```jsx
import { useQuery, useMutation } from '@tanstack/react-query'
import { saveReflection, getStreak } from '@/api/reflection'
import { getTodayCompletedTasks } from '@/api/tasks'
```

Replace the `function Phase2() { ... }` stub with:

```jsx
function Phase2() {
  const [energy, setEnergy] = useState(3)
  const [mood, setMood] = useState(3)
  const [notes, setNotes] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [streak, setStreak] = useState(null)

  const { data: completedTasks = [] } = useQuery({
    queryKey: ['tasks', 'completed-today'],
    queryFn: getTodayCompletedTasks,
  })

  const mutation = useMutation({
    mutationFn: () =>
      saveReflection({ energyRating: energy, moodRating: mood, reflectionNotes: notes || null, isFinalized: true }),
    onSuccess: async () => {
      const data = await getStreak()
      setStreak(data.streak)
      setSubmitted(true)
    },
  })

  const navigate = useNavigate()

  if (submitted) {
    const streakMessage = streak === 1
      ? 'Day 1 — you showed up.'
      : streak > 1
      ? `${streak} days in a row. Keep it going.`
      : 'Good work today.'

    return (
      <div className="text-center py-8">
        <div className="text-4xl mb-3">✨</div>
        <p className="text-xl font-semibold text-gray-900">{streakMessage}</p>
        <p className="mt-2 text-gray-500 text-sm">That's a wrap for today.</p>
        <button
          onClick={() => navigate('/')}
          className="mt-8 px-5 py-2 text-sm rounded-md bg-indigo-600 text-white hover:bg-indigo-700 transition-colors"
        >
          Done
        </button>
      </div>
    )
  }

  const ENERGY_LABELS = { 1: 'Drained', 2: 'Low', 3: 'Okay', 4: 'Good', 5: 'Energized' }
  const MOOD_LABELS = { 1: 'Rough', 2: 'Meh', 3: 'Okay', 4: 'Good', 5: 'Great' }

  return (
    <div>
      <h2 className="text-xl font-semibold text-gray-900 mb-6">How did today go?</h2>

      {completedTasks.length > 0 && (
        <div className="mb-6">
          <p className="text-xs font-medium text-gray-400 uppercase tracking-wider mb-2">
            Completed today
          </p>
          <ul className="space-y-1">
            {completedTasks.map((t) => (
              <li key={t.id} className="text-sm text-gray-700 flex items-center gap-2">
                <span className="text-indigo-400">✓</span>
                {t.title}
              </li>
            ))}
          </ul>
        </div>
      )}

      <form onSubmit={(e) => { e.preventDefault(); mutation.mutate() }} className="space-y-6">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Energy — <span className="text-indigo-600">{ENERGY_LABELS[energy]}</span>
          </label>
          <input
            type="range" min="1" max="5" step="1"
            value={energy}
            onChange={(e) => setEnergy(Number(e.target.value))}
            className="w-full accent-indigo-600"
          />
          <div className="flex justify-between text-xs text-gray-400 mt-1">
            <span>Drained</span><span>Energized</span>
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Mood — <span className="text-indigo-600">{MOOD_LABELS[mood]}</span>
          </label>
          <input
            type="range" min="1" max="5" step="1"
            value={mood}
            onChange={(e) => setMood(Number(e.target.value))}
            className="w-full accent-indigo-600"
          />
          <div className="flex justify-between text-xs text-gray-400 mt-1">
            <span>Rough</span><span>Great</span>
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Anything on your mind? <span className="text-gray-400 font-normal">(optional)</span>
          </label>
          <textarea
            rows={3}
            placeholder="Anything on your mind?"
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 resize-none"
          />
        </div>

        {mutation.isError && (
          <p className="text-sm text-red-500">Something went wrong. Try again.</p>
        )}

        <button
          type="submit"
          disabled={mutation.isPending}
          className="w-full py-2.5 text-sm rounded-md bg-indigo-600 text-white hover:bg-indigo-700 disabled:opacity-50 transition-colors font-medium"
        >
          {mutation.isPending ? 'Saving…' : 'Wrap up the day'}
        </button>
      </form>
    </div>
  )
}
```

Note: `Phase2` uses `useState` and `useNavigate` which are already imported in the outer `EndDayPage.jsx` file. `Phase2` is defined inside the same file, so it inherits those imports.

- [ ] **Step 2: Commit**

```bash
git add frontend/src/pages/EndDayPage.jsx
git commit -m "feat: add End Day Phase 2 — reflection form and streak display (Slice 3)"
```

---

## Task 12: Routing + Start Day Stub

**Files:**
- Create: `frontend/src/pages/StartDayPage.jsx`
- Modify: `frontend/src/App.jsx`

- [ ] **Step 1: Create `StartDayPage.jsx`**

```jsx
export function StartDayPage() {
  return (
    <div className="p-8">
      <h1 className="text-2xl font-semibold text-gray-900">Start Day</h1>
      <p className="mt-2 text-gray-500">Coming soon.</p>
    </div>
  )
}
```

- [ ] **Step 2: Update `App.jsx`**

Add imports:
```jsx
import { EndDayPage } from '@/pages/EndDayPage'
import { StartDayPage } from '@/pages/StartDayPage'
```

Add routes inside the `<Route element={<AppLayout />}>` block:
```jsx
<Route path="/end-day" element={<EndDayPage />} />
<Route path="/start-day" element={<StartDayPage />} />
```

- [ ] **Step 3: Update the implementation plan checklist**

Open `docs/planning/2026-03-30-implementation-plan-design.md` and check off all Slice 3 items.

- [ ] **Step 4: Run the app end-to-end**

```bash
docker-compose up -d
cd backend && ./mvnw spring-boot:run &
cd frontend && npm run dev
```

Verify:
1. Sidebar shows "Daily rituals" group with Start Day and End Day
2. Inbox shows deferred items with Convert/Defer/Dismiss actions
3. Convert creates a task and removes the item from the list
4. Defer removes the item from the list
5. Dismiss (two-click) removes the item from the list
6. End Day → Phase 1 shows items one at a time, advances, shows "Inbox Zero!" on last item
7. End Day → Phase 2 shows reflection sliders, completed tasks, saves and shows streak

- [ ] **Step 5: Run all backend tests**

```bash
cd backend && ./mvnw test -q 2>&1 | tail -10
```
Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add frontend/src/pages/StartDayPage.jsx frontend/src/App.jsx \
        docs/planning/2026-03-30-implementation-plan-design.md
git commit -m "feat: add routing for End Day and Start Day stub, complete Slice 3"
```
