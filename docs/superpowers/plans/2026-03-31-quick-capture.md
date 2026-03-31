# Quick Capture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a low-friction thought-capture flow: sidebar button + Ctrl+Space hotkey opens a modal that saves raw text as a `DeferredItem`, confirms with a chime + checkmark, and shows a badge count on the Inbox nav link.

**Architecture:** A new `deferred` package mirrors existing `task`/`project` packages (entity → repository → service → controller). On the frontend, a single self-contained `QuickCapture.jsx` component owns the sidebar button, Radix UI modal, Web Audio chime, and hotkey listener; it is mounted once in `AppLayout.jsx`, which also gains an inbox badge query.

**Tech Stack:** Java 21, Spring Boot 3.2, Spring Data JPA (JPQL `@Query`), Flyway; React 18, TanStack Query v5, Radix UI Dialog, Tailwind CSS, Web Audio API.

---

## File Map

| File | Status | Purpose |
|---|---|---|
| `backend/src/main/resources/db/migration/V4__create_deferred_item.sql` | **Create** | Schema for `deferred_item` table |
| `backend/src/main/java/com/planner/backend/deferred/DeferredItem.java` | **Create** | JPA entity |
| `backend/src/main/java/com/planner/backend/deferred/DeferredItemRepository.java` | **Create** | JPA repository with pending-items query |
| `backend/src/main/java/com/planner/backend/deferred/dto/DeferredItemCreateRequest.java` | **Create** | Validated request record |
| `backend/src/main/java/com/planner/backend/deferred/dto/DeferredItemResponse.java` | **Create** | Response record with static `from()` factory |
| `backend/src/main/java/com/planner/backend/deferred/DeferredItemService.java` | **Create** | `create()` and `listPending()` business logic |
| `backend/src/main/java/com/planner/backend/deferred/DeferredItemController.java` | **Create** | `POST /api/v1/deferred`, `GET /api/v1/deferred` |
| `backend/src/main/java/com/planner/backend/deferred/DeferredItemExceptionHandler.java` | **Create** | Placeholder `@RestControllerAdvice` (populated in Evening Ritual slice) |
| `backend/src/test/java/com/planner/backend/deferred/DeferredItemControllerIntegrationTest.java` | **Create** | `@WebMvcTest` covering create + list + auth |
| `frontend/src/api/deferred.js` | **Create** | `createDeferredItem` and `getDeferredItems` API functions |
| `frontend/src/components/QuickCapture.jsx` | **Create** | Self-contained capture button + modal + chime |
| `frontend/src/layouts/AppLayout.jsx` | **Modify** | Add `<QuickCapture />` + inbox badge query |

---

## Task 1: Database Migration

**Files:**
- Create: `backend/src/main/resources/db/migration/V4__create_deferred_item.sql`

- [ ] **Step 1: Create the migration file**

```sql
CREATE TABLE deferred_item (
    id                   UUID                     NOT NULL DEFAULT gen_random_uuid(),
    user_id              UUID                     NOT NULL,
    raw_text             TEXT                     NOT NULL,
    is_processed         BOOLEAN                  NOT NULL DEFAULT false,
    captured_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at         TIMESTAMP WITH TIME ZONE,
    resolved_task_id     UUID,
    resolved_project_id  UUID,
    deferred_until_date  DATE,
    deferral_count       INTEGER                  NOT NULL DEFAULT 0,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT pk_deferred_item PRIMARY KEY (id),
    CONSTRAINT fk_deferred_item_user FOREIGN KEY (user_id) REFERENCES app_user(id),
    CONSTRAINT fk_deferred_item_task FOREIGN KEY (resolved_task_id) REFERENCES task(id),
    CONSTRAINT fk_deferred_item_project FOREIGN KEY (resolved_project_id) REFERENCES project(id)
);

CREATE INDEX idx_deferred_item_user_id ON deferred_item(user_id);
CREATE INDEX idx_deferred_item_is_processed ON deferred_item(is_processed);

CREATE TRIGGER trg_deferred_item_updated_at
BEFORE UPDATE ON deferred_item
FOR EACH ROW EXECUTE FUNCTION set_updated_at();
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/resources/db/migration/V4__create_deferred_item.sql
git commit -m "feat: add V4 migration for deferred_item table"
```

---

## Task 2: DeferredItem Entity, DTOs, and Repository

**Files:**
- Create: `backend/src/main/java/com/planner/backend/deferred/DeferredItem.java`
- Create: `backend/src/main/java/com/planner/backend/deferred/dto/DeferredItemCreateRequest.java`
- Create: `backend/src/main/java/com/planner/backend/deferred/dto/DeferredItemResponse.java`
- Create: `backend/src/main/java/com/planner/backend/deferred/DeferredItemRepository.java`

- [ ] **Step 1: Create `DeferredItem.java`**

```java
package com.planner.backend.deferred;

import com.planner.backend.auth.AppUser;
import com.planner.backend.project.Project;
import com.planner.backend.task.Task;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "deferred_item")
public class DeferredItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "raw_text", nullable = false, columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "is_processed", nullable = false)
    private boolean isProcessed = false;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_task_id")
    private Task resolvedTask;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_project_id")
    private Project resolvedProject;

    @Column(name = "deferred_until_date")
    private LocalDate deferredUntilDate;

    @Column(name = "deferral_count", nullable = false)
    private int deferralCount = 0;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    public DeferredItem() {}

    public DeferredItem(AppUser user, String rawText) {
        this.user = user;
        this.rawText = rawText;
        this.capturedAt = Instant.now();
    }

    // Getters

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public String getRawText() { return rawText; }
    public boolean isProcessed() { return isProcessed; }
    public Instant getCapturedAt() { return capturedAt; }
    public Instant getProcessedAt() { return processedAt; }
    public Task getResolvedTask() { return resolvedTask; }
    public Project getResolvedProject() { return resolvedProject; }
    public LocalDate getDeferredUntilDate() { return deferredUntilDate; }
    public int getDeferralCount() { return deferralCount; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // Setters (only fields mutated in this slice and future Evening Ritual slice)

    public void setProcessed(boolean processed) { isProcessed = processed; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
    public void setResolvedTask(Task resolvedTask) { this.resolvedTask = resolvedTask; }
    public void setResolvedProject(Project resolvedProject) { this.resolvedProject = resolvedProject; }
    public void setDeferredUntilDate(LocalDate deferredUntilDate) { this.deferredUntilDate = deferredUntilDate; }
    public void setDeferralCount(int deferralCount) { this.deferralCount = deferralCount; }
}
```

- [ ] **Step 2: Create `DeferredItemCreateRequest.java`**

```java
package com.planner.backend.deferred.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeferredItemCreateRequest(
        @NotBlank @Size(max = 2000) String rawText
) {}
```

- [ ] **Step 3: Create `DeferredItemResponse.java`**

```java
package com.planner.backend.deferred.dto;

import com.planner.backend.deferred.DeferredItem;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DeferredItemResponse(
        UUID id,
        UUID userId,
        String rawText,
        boolean isProcessed,
        Instant capturedAt,
        Instant processedAt,
        UUID resolvedTaskId,
        UUID resolvedProjectId,
        LocalDate deferredUntilDate,
        int deferralCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static DeferredItemResponse from(DeferredItem item) {
        return new DeferredItemResponse(
                item.getId(),
                item.getUser().getId(),
                item.getRawText(),
                item.isProcessed(),
                item.getCapturedAt(),
                item.getProcessedAt(),
                item.getResolvedTask() != null ? item.getResolvedTask().getId() : null,
                item.getResolvedProject() != null ? item.getResolvedProject().getId() : null,
                item.getDeferredUntilDate(),
                item.getDeferralCount(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
```

- [ ] **Step 4: Create `DeferredItemRepository.java`**

```java
package com.planner.backend.deferred;

import com.planner.backend.auth.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DeferredItemRepository extends JpaRepository<DeferredItem, UUID> {

    @Query("""
            SELECT d FROM DeferredItem d
            WHERE d.user = :user
              AND d.isProcessed = false
              AND (d.deferredUntilDate IS NULL OR d.deferredUntilDate <= :today)
            ORDER BY d.capturedAt ASC
            """)
    List<DeferredItem> findPendingForUser(@Param("user") AppUser user, @Param("today") LocalDate today);
}
```

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/planner/backend/deferred/
git commit -m "feat: add DeferredItem entity, DTOs, and repository"
```

---

## Task 3: DeferredItemService

**Files:**
- Create: `backend/src/main/java/com/planner/backend/deferred/DeferredItemService.java`

- [ ] **Step 1: Create `DeferredItemService.java`**

```java
package com.planner.backend.deferred;

import com.planner.backend.auth.AppUser;
import com.planner.backend.deferred.dto.DeferredItemCreateRequest;
import com.planner.backend.deferred.dto.DeferredItemResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional
public class DeferredItemService {

    private final DeferredItemRepository repository;

    public DeferredItemService(DeferredItemRepository repository) {
        this.repository = repository;
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
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/planner/backend/deferred/DeferredItemService.java
git commit -m "feat: add DeferredItemService with create and listPending"
```

---

## Task 4: DeferredItemController, ExceptionHandler, and Integration Test

**Files:**
- Create: `backend/src/main/java/com/planner/backend/deferred/DeferredItemController.java`
- Create: `backend/src/main/java/com/planner/backend/deferred/DeferredItemExceptionHandler.java`
- Create: `backend/src/test/java/com/planner/backend/deferred/DeferredItemControllerIntegrationTest.java`

- [ ] **Step 1: Write the failing integration test**

```java
package com.planner.backend.deferred;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planner.backend.auth.AppUser;
import com.planner.backend.auth.AppUserRepository;
import com.planner.backend.auth.JwtAuthFilter;
import com.planner.backend.auth.JwtService;
import com.planner.backend.auth.SecurityConfig;
import com.planner.backend.deferred.dto.DeferredItemCreateRequest;
import com.planner.backend.deferred.dto.DeferredItemResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DeferredItemController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthFilter.class, DeferredItemExceptionHandler.class})
class DeferredItemControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private AppUserRepository userRepository;

    @MockBean
    private DeferredItemService deferredItemService;

    private AppUser user;
    private String accessToken;

    @BeforeEach
    void setUp() {
        user = new AppUser("alice@example.com", "hash", "Alice", "UTC");
        accessToken = jwtService.generateAccessToken("alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
    }

    // --- POST /api/v1/deferred ---

    @Test
    void create_validRequest_returns201WithItem() throws Exception {
        DeferredItemCreateRequest req = new DeferredItemCreateRequest("Buy oat milk");
        DeferredItemResponse resp = new DeferredItemResponse(
                UUID.randomUUID(), user.getId(), "Buy oat milk",
                false, Instant.now(), null, null, null, null, 0,
                Instant.now(), Instant.now()
        );
        when(deferredItemService.create(any(AppUser.class), any(DeferredItemCreateRequest.class)))
                .thenReturn(resp);

        mockMvc.perform(post("/api/v1/deferred")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rawText").value("Buy oat milk"))
                .andExpect(jsonPath("$.isProcessed").value(false));
    }

    @Test
    void create_noAuth_returns401() throws Exception {
        DeferredItemCreateRequest req = new DeferredItemCreateRequest("Unauth thought");

        mockMvc.perform(post("/api/v1/deferred")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_blankRawText_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/deferred")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rawText\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    // --- GET /api/v1/deferred ---

    @Test
    void listPending_validAuth_returns200WithItems() throws Exception {
        DeferredItemResponse item = new DeferredItemResponse(
                UUID.randomUUID(), user.getId(), "Call dentist",
                false, Instant.now(), null, null, null, null, 0,
                Instant.now(), Instant.now()
        );
        when(deferredItemService.listPending(any(AppUser.class))).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/deferred")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].rawText").value("Call dentist"))
                .andExpect(jsonPath("$[0].isProcessed").value(false));
    }

    @Test
    void listPending_noAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/deferred"))
                .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 2: Run the test — expect it to fail (no controller exists yet)**

```bash
cd backend
./mvnw test -pl . -Dtest=DeferredItemControllerIntegrationTest -q
```

Expected: COMPILATION ERROR — `DeferredItemController` and `DeferredItemExceptionHandler` do not exist yet.

- [ ] **Step 3: Create `DeferredItemController.java`**

```java
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
```

- [ ] **Step 4: Create `DeferredItemExceptionHandler.java`**

```java
package com.planner.backend.deferred;

import org.springframework.web.bind.annotation.RestControllerAdvice;

// No domain exceptions in this slice.
// Exceptions for convert/defer/dismiss will be added in the Evening Ritual slice.
@RestControllerAdvice
public class DeferredItemExceptionHandler {
}
```

- [ ] **Step 5: Run the test — expect it to pass**

```bash
cd backend
./mvnw test -pl . -Dtest=DeferredItemControllerIntegrationTest -q
```

Expected: `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 6: Run all backend tests to confirm nothing is broken**

```bash
cd backend
./mvnw test -q
```

Expected: `Tests run: N, Failures: 0, Errors: 0, Skipped: 0`

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/planner/backend/deferred/DeferredItemController.java \
        backend/src/main/java/com/planner/backend/deferred/DeferredItemExceptionHandler.java \
        backend/src/test/java/com/planner/backend/deferred/DeferredItemControllerIntegrationTest.java
git commit -m "feat: add DeferredItemController with POST and GET endpoints"
```

---

## Task 5: Frontend API Module

**Files:**
- Create: `frontend/src/api/deferred.js`

- [ ] **Step 1: Create `deferred.js`**

```js
import { authFetch, handleResponse } from './client'

const BASE = '/api/v1'

export async function createDeferredItem(rawText) {
  const res = await authFetch(`${BASE}/deferred`, {
    method: 'POST',
    body: JSON.stringify({ rawText }),
  })
  return handleResponse(res)
}

export async function getDeferredItems() {
  const res = await authFetch(`${BASE}/deferred`)
  return handleResponse(res)
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/api/deferred.js
git commit -m "feat: add deferred items API module"
```

---

## Task 6: QuickCapture Component

**Files:**
- Create: `frontend/src/components/QuickCapture.jsx`

- [ ] **Step 1: Create `QuickCapture.jsx`**

```jsx
import { useState, useEffect } from 'react'
import * as Dialog from '@radix-ui/react-dialog'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createDeferredItem } from '@/api/deferred'

function playChime() {
  try {
    const ctx = new AudioContext()
    const osc = ctx.createOscillator()
    const gain = ctx.createGain()
    osc.connect(gain)
    gain.connect(ctx.destination)
    osc.frequency.value = 880
    osc.type = 'sine'
    gain.gain.setValueAtTime(0.3, ctx.currentTime)
    gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.2)
    osc.start(ctx.currentTime)
    osc.stop(ctx.currentTime + 0.2)
    osc.onended = () => ctx.close()
  } catch {
    // AudioContext unavailable (e.g. in test environments) — silently ignore
  }
}

export function QuickCapture() {
  const [open, setOpen] = useState(false)
  const [text, setText] = useState('')
  const [confirmed, setConfirmed] = useState(false)
  const [error, setError] = useState(null)

  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: () => createDeferredItem(text.trim()),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['deferred'] })
      playChime()
      setConfirmed(true)
      setTimeout(() => {
        setOpen(false)
        setConfirmed(false)
        setText('')
        setError(null)
      }, 1000)
    },
    onError: () => {
      setError("Couldn't save — try again.")
    },
  })

  // Ctrl+Space global hotkey — toggles modal open/closed
  useEffect(() => {
    function handleKeyDown(e) {
      if (e.ctrlKey && e.code === 'Space') {
        e.preventDefault()
        setOpen(prev => !prev)
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [])

  function handleOpenChange(next) {
    if (!next) {
      setOpen(false)
      setText('')
      setConfirmed(false)
      setError(null)
    } else {
      setOpen(true)
    }
  }

  function handleTextareaKeyDown(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      if (text.trim() && !mutation.isPending) mutation.mutate()
    }
  }

  const isBlank = !text.trim()

  return (
    <Dialog.Root open={open} onOpenChange={handleOpenChange}>
      <Dialog.Trigger asChild>
        <button
          className="w-full text-left px-3 py-2 rounded-md text-sm font-medium text-indigo-600 hover:bg-indigo-50 hover:text-indigo-700 transition-colors duration-100 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-1"
        >
          + Quick capture
        </button>
      </Dialog.Trigger>

      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 bg-black/30 z-40" />
        <Dialog.Content
          className="fixed left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 z-50 w-full max-w-md bg-white rounded-xl shadow-xl p-6 focus:outline-none"
          aria-label="Quick capture"
        >
          {confirmed ? (
            <div className="flex flex-col items-center gap-3 py-4">
              <svg
                className="text-green-500 w-10 h-10"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                strokeWidth={2}
                aria-hidden="true"
              >
                <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
              </svg>
              <p className="text-gray-700 font-medium">Captured.</p>
            </div>
          ) : (
            <>
              <Dialog.Title className="text-base font-semibold text-gray-900 mb-3">
                Quick capture
              </Dialog.Title>

              <textarea
                autoFocus
                rows={3}
                className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm text-gray-900 placeholder-gray-400 resize-none focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                placeholder="What's on your mind?"
                value={text}
                onChange={e => { setText(e.target.value); setError(null) }}
                onKeyDown={handleTextareaKeyDown}
              />

              {error && (
                <p className="mt-1 text-xs text-red-600" role="alert">{error}</p>
              )}

              <div className="mt-4 flex justify-end gap-2">
                <Dialog.Close asChild>
                  <button className="px-4 py-2 text-sm text-gray-600 hover:text-gray-900 focus:outline-none focus:ring-2 focus:ring-indigo-500 rounded-md transition-colors duration-100">
                    Cancel
                  </button>
                </Dialog.Close>
                <button
                  disabled={isBlank || mutation.isPending}
                  onClick={() => mutation.mutate()}
                  className="px-4 py-2 text-sm font-medium text-white bg-indigo-600 rounded-md hover:bg-indigo-700 disabled:opacity-50 disabled:cursor-not-allowed focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-1 transition-colors duration-100"
                >
                  {mutation.isPending ? 'Saving…' : 'Capture'}
                </button>
              </div>
            </>
          )}
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  )
}
```

- [ ] **Step 2: Start the dev server and manually verify the component**

```bash
cd frontend
npm run dev
```

Open http://localhost:5173. Log in. Verify:
- Sidebar shows `+ Quick capture` button above the logout button
- Clicking it opens the modal (not yet — button is not wired to AppLayout yet; this step confirms no import errors once wired in Task 7)

> Note: The component won't appear in the UI until it is mounted in `AppLayout.jsx` (Task 7). This commit just makes the component available.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/QuickCapture.jsx
git commit -m "feat: add QuickCapture component with modal, chime, and hotkey"
```

---

## Task 7: AppLayout Integration — Badge + QuickCapture

**Files:**
- Modify: `frontend/src/layouts/AppLayout.jsx`

- [ ] **Step 1: Open `AppLayout.jsx` and replace its contents with the updated version**

The changes are:
1. Add three imports at the top (`useQuery`, `getDeferredItems`, `QuickCapture`)
2. Add badge prop to `NavItem`
3. Add `inboxCount` query in `AppLayout`
4. Pass `badge={inboxCount}` to the Inbox nav item
5. Render `<QuickCapture />` in the sidebar

```jsx
import { NavLink } from 'react-router-dom'
import { Outlet } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { useAuth } from '@/auth/useAuth'
import { getDeferredItems } from '@/api/deferred'
import { QuickCapture } from '@/components/QuickCapture'

const NAV_ITEMS = [
  {
    label: 'Dashboard',
    to: '/',
    end: true,
    icon: (
      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none"
        stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
        aria-hidden="true">
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
        stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
        aria-hidden="true">
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
        stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
        aria-hidden="true">
        <path d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z" />
      </svg>
    ),
  },
  {
    label: 'Inbox',
    to: '/inbox',
    icon: (
      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none"
        stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
        aria-hidden="true">
        <polyline points="22 12 16 12 14 15 10 15 8 12 2 12" />
        <path d="M5.45 5.11L2 12v6a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-6l-3.45-6.89A2 2 0 0 0 16.76 4H7.24a2 2 0 0 0-1.79 1.11z" />
      </svg>
    ),
  },
]

function NavItem({ item, badge = 0 }) {
  return (
    <NavLink
      to={item.to}
      end={item.end}
      className={({ isActive }) =>
        [
          'flex items-center gap-3 px-3 py-2 rounded-md text-sm font-medium transition-colors duration-100 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-1',
          isActive
            ? 'bg-indigo-50 text-indigo-700'
            : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900',
        ].join(' ')
      }
    >
      {item.icon}
      {item.label}
      {badge > 0 && (
        <span className="ml-auto bg-indigo-100 text-indigo-700 text-xs font-semibold rounded-full px-1.5 py-0.5 min-w-[1.25rem] text-center">
          {badge}
        </span>
      )}
    </NavLink>
  )
}

export function AppLayout() {
  const { user, logout } = useAuth()
  const displayName = user?.displayName || user?.email || 'User'

  const { data: deferredItems = [] } = useQuery({
    queryKey: ['deferred'],
    queryFn: getDeferredItems,
  })
  const inboxCount = deferredItems.length

  return (
    <div className="flex h-screen bg-gray-50">
      {/* Sidebar */}
      <nav
        className="w-60 flex-shrink-0 flex flex-col bg-white border-r border-gray-200"
        aria-label="Main navigation"
      >
        {/* Logo / Brand */}
        <div className="px-5 py-5 border-b border-gray-100">
          <span className="text-lg font-semibold text-gray-900 tracking-tight">Planner</span>
        </div>

        {/* Nav links */}
        <div className="flex-1 overflow-y-auto px-3 py-4 space-y-1">
          {NAV_ITEMS.map((item) => (
            <NavItem
              key={item.to}
              item={item}
              badge={item.to === '/inbox' ? inboxCount : 0}
            />
          ))}
        </div>

        {/* Quick capture + user + logout */}
        <div className="px-4 py-4 border-t border-gray-100 space-y-2">
          <QuickCapture />
          <p className="text-xs text-gray-500 truncate" title={displayName}>
            {displayName}
          </p>
          <button
            onClick={logout}
            className="w-full text-left px-3 py-2 rounded-md text-sm text-gray-600 hover:bg-gray-100 hover:text-gray-800 transition-colors duration-100 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-1"
          >
            Log out
          </button>
        </div>
      </nav>

      {/* Main content */}
      <main className="flex-1 overflow-y-auto">
        <Outlet />
      </main>
    </div>
  )
}
```

- [ ] **Step 2: Verify in the browser**

With the dev server running (http://localhost:5173), log in and confirm:
1. `+ Quick capture` button appears at the bottom of the sidebar above the display name
2. Clicking it opens the modal with an autofocused textarea
3. Typing a thought and pressing `Enter` (or clicking `Capture`) shows the green checkmark and plays a brief chime, then the modal closes after ~1 second
4. `Escape` dismisses the modal without saving
5. `Ctrl+Space` from any page opens and closes the modal
6. If the backend is running with a real database, after capturing an item the Inbox nav link shows a badge count of 1
7. Submitting with a blank/whitespace input does nothing (Capture button stays disabled)

- [ ] **Step 3: Commit**

```bash
git add frontend/src/layouts/AppLayout.jsx
git commit -m "feat: integrate QuickCapture and inbox badge into AppLayout"
```

---

## Done

All backend and frontend work for Slice 2 is complete. The `deferred_item` table is created, two API endpoints are live and tested, and the UI provides a sidebar button + Ctrl+Space hotkey for capturing thoughts with a chime confirmation and inbox badge.

**What's NOT included in this slice (intentionally):**
- Processing deferred items (convert to task / defer / dismiss) → Evening Ritual slice
- `InboxPage.jsx` listing items for review → Evening Ritual slice
- `DeferredItemExceptionHandler` domain exceptions → Evening Ritual slice
