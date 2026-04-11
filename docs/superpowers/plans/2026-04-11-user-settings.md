# User Settings Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers-extended-cc:subagent-driven-development (recommended) or superpowers-extended-cc:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a user preferences page where users can configure their preferred name, schedule defaults, week start day, and end-of-week ceremony day.

**Architecture:** New columns on `app_user` table (12 total: 5 active, 7 dormant). New `GET`/`PATCH` endpoints under `/api/v1/user/preferences`. New React settings page at `/settings` with TanStack Query integration. Wire preferences into existing schedule service and ritual components.

**Tech Stack:** Java 21, Spring Boot 3.x, Spring Data JPA, Flyway, PostgreSQL, React 18, TanStack Query, Radix UI, Tailwind CSS

**Spec:** `docs/superpowers/specs/2026-04-11-user-settings-design.md`

---

### Task 1: Flyway Migration — Add Preference Columns to app_user

**Goal:** Add 12 new columns to `app_user` with defaults and CHECK constraints so existing users get sensible values.

**Files:**
- Create: `backend/src/main/resources/db/migration/V11__add_user_preferences.sql`

**Acceptance Criteria:**
- [ ] Migration runs without error on existing database
- [ ] All 12 columns added with correct types and defaults
- [ ] CHECK constraints enforce valid ranges
- [ ] Existing users get default values automatically

**Verify:** `cd backend && mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/planner -Dflyway.user=planner -Dflyway.password=planner` → no errors. Then verify with: `docker exec planner-db psql -U planner -d planner -c "\d app_user"` → shows all new columns.

**Steps:**

- [ ] **Step 1: Create migration file**

```sql
-- V11__add_user_preferences.sql

-- Active preferences (exposed in settings UI)
ALTER TABLE app_user ADD COLUMN default_start_time    TIME     NOT NULL DEFAULT '08:00';
ALTER TABLE app_user ADD COLUMN default_end_time      TIME     NOT NULL DEFAULT '17:00';
ALTER TABLE app_user ADD COLUMN default_session_minutes SMALLINT NOT NULL DEFAULT 60;
ALTER TABLE app_user ADD COLUMN week_start_day         SMALLINT NOT NULL DEFAULT 1;
ALTER TABLE app_user ADD COLUMN ceremony_day           SMALLINT NOT NULL DEFAULT 5;

-- Dormant preferences (columns for future features, no UI yet)
ALTER TABLE app_user ADD COLUMN timer_warning_minutes  SMALLINT NOT NULL DEFAULT 5;
ALTER TABLE app_user ADD COLUMN timer_type             VARCHAR(10) NOT NULL DEFAULT 'countdown';
ALTER TABLE app_user ADD COLUMN enable_chime           BOOLEAN  NOT NULL DEFAULT true;
ALTER TABLE app_user ADD COLUMN quick_capture_keep_open BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE app_user ADD COLUMN max_daily_tasks        SMALLINT;
ALTER TABLE app_user ADD COLUMN streak_celebration_threshold SMALLINT;
ALTER TABLE app_user ADD COLUMN locale                 VARCHAR(10) NOT NULL DEFAULT 'en';

-- Constraints
ALTER TABLE app_user ADD CONSTRAINT chk_week_start_day
    CHECK (week_start_day BETWEEN 1 AND 7);

ALTER TABLE app_user ADD CONSTRAINT chk_ceremony_day
    CHECK (ceremony_day BETWEEN 1 AND 7);

ALTER TABLE app_user ADD CONSTRAINT chk_default_session_minutes
    CHECK (default_session_minutes > 0 AND default_session_minutes <= 240
           AND default_session_minutes % 15 = 0);

ALTER TABLE app_user ADD CONSTRAINT chk_timer_type
    CHECK (timer_type IN ('countdown', 'countup'));

ALTER TABLE app_user ADD CONSTRAINT chk_timer_warning_minutes
    CHECK (timer_warning_minutes > 0 AND timer_warning_minutes <= 60);

ALTER TABLE app_user ADD CONSTRAINT chk_default_times
    CHECK (default_start_time < default_end_time);
```

- [ ] **Step 2: Run migration and verify**

Run: `cd backend && mvn spring-boot:run` (Flyway runs automatically on startup)
Expected: Application starts, logs show `Successfully applied 1 migration to schema "public", now at version v11`

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/resources/db/migration/V11__add_user_preferences.sql
git commit -m "feat: add user preference columns to app_user (V11 migration)"
```

---

### Task 2: Backend Entity, DTOs, and Service

**Goal:** Extend `AppUser` with preference fields and create the `UserPreferencesService` with DTOs for reading and updating preferences.

**Files:**
- Modify: `backend/src/main/java/com/echel/planner/backend/auth/AppUser.java`
- Create: `backend/src/main/java/com/echel/planner/backend/auth/dto/PreferencesResponse.java`
- Create: `backend/src/main/java/com/echel/planner/backend/auth/dto/UpdatePreferencesRequest.java`
- Create: `backend/src/main/java/com/echel/planner/backend/auth/UserPreferencesService.java`
- Test: `backend/src/test/java/com/echel/planner/backend/auth/UserPreferencesServiceTest.java`

**Acceptance Criteria:**
- [ ] AppUser has getters/setters for all 5 active preference fields
- [ ] PreferencesResponse record maps DayOfWeek as enum names (MONDAY, FRIDAY, etc.)
- [ ] UpdatePreferencesRequest allows partial updates (all fields nullable)
- [ ] Service validates timezone, time ordering, and session-minute alignment
- [ ] Service throws PreferencesValidationException for invalid input
- [ ] Unit tests cover validation rules

**Verify:** `cd backend && mvn test -pl . -Dtest=UserPreferencesServiceTest` → all tests pass

**Steps:**

- [ ] **Step 1: Write the service unit test**

Create `backend/src/test/java/com/echel/planner/backend/auth/UserPreferencesServiceTest.java`:

```java
package com.echel.planner.backend.auth;

import com.echel.planner.backend.auth.dto.PreferencesResponse;
import com.echel.planner.backend.auth.dto.UpdatePreferencesRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPreferencesServiceTest {

    @Mock
    private AppUserRepository userRepository;

    private UserPreferencesService service;

    private AppUser testUser;

    @BeforeEach
    void setUp() {
        service = new UserPreferencesService(userRepository);
        testUser = new AppUser("test@example.com", "hash", "Test User", "UTC");
    }

    @Test
    void getPreferences_returnsCurrentValues() {
        PreferencesResponse response = service.getPreferences(testUser);

        assertThat(response.displayName()).isEqualTo("Test User");
        assertThat(response.timezone()).isEqualTo("UTC");
        assertThat(response.defaultStartTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(response.defaultEndTime()).isEqualTo(LocalTime.of(17, 0));
        assertThat(response.defaultSessionMinutes()).isEqualTo(60);
        assertThat(response.weekStartDay()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(response.ceremonyDay()).isEqualTo(DayOfWeek.FRIDAY);
    }

    @Test
    void updatePreferences_partialUpdate_onlyChangesSpecifiedFields() {
        var request = new UpdatePreferencesRequest(
                null, null, null, null, null, null, DayOfWeek.SUNDAY
        );

        when(userRepository.save(testUser)).thenReturn(testUser);

        PreferencesResponse response = service.updatePreferences(testUser, request);

        assertThat(response.ceremonyDay()).isEqualTo(DayOfWeek.SUNDAY);
        assertThat(response.displayName()).isEqualTo("Test User"); // unchanged
        verify(userRepository).save(testUser);
    }

    @Test
    void updatePreferences_invalidTimezone_throws() {
        var request = new UpdatePreferencesRequest(
                null, "Not/A/Zone", null, null, null, null, null
        );

        assertThatThrownBy(() -> service.updatePreferences(testUser, request))
                .isInstanceOf(UserPreferencesService.PreferencesValidationException.class)
                .hasMessageContaining("timezone");
    }

    @Test
    void updatePreferences_startTimeAfterEndTime_throws() {
        var request = new UpdatePreferencesRequest(
                null, null, LocalTime.of(18, 0), LocalTime.of(9, 0), null, null, null
        );

        assertThatThrownBy(() -> service.updatePreferences(testUser, request))
                .isInstanceOf(UserPreferencesService.PreferencesValidationException.class)
                .hasMessageContaining("before");
    }

    @Test
    void updatePreferences_sessionMinutesNotMultipleOf15_throws() {
        var request = new UpdatePreferencesRequest(
                null, null, null, null, 25, null, null
        );

        assertThatThrownBy(() -> service.updatePreferences(testUser, request))
                .isInstanceOf(UserPreferencesService.PreferencesValidationException.class)
                .hasMessageContaining("multiple of 15");
    }

    @Test
    void updatePreferences_blankDisplayName_throws() {
        var request = new UpdatePreferencesRequest(
                "   ", null, null, null, null, null, null
        );

        assertThatThrownBy(() -> service.updatePreferences(testUser, request))
                .isInstanceOf(UserPreferencesService.PreferencesValidationException.class)
                .hasMessageContaining("display name");
    }

    @Test
    void updatePreferences_timeNotAlignedTo15Minutes_throws() {
        var request = new UpdatePreferencesRequest(
                null, null, LocalTime.of(8, 7), null, null, null, null
        );

        assertThatThrownBy(() -> service.updatePreferences(testUser, request))
                .isInstanceOf(UserPreferencesService.PreferencesValidationException.class)
                .hasMessageContaining("15-minute");
    }

    @Test
    void updatePreferences_allFieldsSet_updatesEverything() {
        var request = new UpdatePreferencesRequest(
                "New Name", "America/New_York",
                LocalTime.of(9, 0), LocalTime.of(18, 0),
                45, DayOfWeek.SUNDAY, DayOfWeek.THURSDAY
        );

        when(userRepository.save(testUser)).thenReturn(testUser);

        PreferencesResponse response = service.updatePreferences(testUser, request);

        assertThat(response.displayName()).isEqualTo("New Name");
        assertThat(response.timezone()).isEqualTo("America/New_York");
        assertThat(response.defaultStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(response.defaultEndTime()).isEqualTo(LocalTime.of(18, 0));
        assertThat(response.defaultSessionMinutes()).isEqualTo(45);
        assertThat(response.weekStartDay()).isEqualTo(DayOfWeek.SUNDAY);
        assertThat(response.ceremonyDay()).isEqualTo(DayOfWeek.THURSDAY);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd backend && mvn test -pl . -Dtest=UserPreferencesServiceTest`
Expected: Compilation errors (classes don't exist yet)

- [ ] **Step 3: Add preference fields to AppUser**

Modify `backend/src/main/java/com/echel/planner/backend/auth/AppUser.java`. Add these fields after the `timezone` field (line 30):

```java
import java.time.DayOfWeek;
import java.time.LocalTime;
```

```java
    @Column(name = "default_start_time", nullable = false)
    private LocalTime defaultStartTime = LocalTime.of(8, 0);

    @Column(name = "default_end_time", nullable = false)
    private LocalTime defaultEndTime = LocalTime.of(17, 0);

    @Column(name = "default_session_minutes", nullable = false)
    private int defaultSessionMinutes = 60;

    @Column(name = "week_start_day", nullable = false)
    private int weekStartDay = 1;

    @Column(name = "ceremony_day", nullable = false)
    private int ceremonyDay = 5;
```

Add getters that return typed values and setters:

```java
    public LocalTime getDefaultStartTime() { return defaultStartTime; }
    public void setDefaultStartTime(LocalTime defaultStartTime) { this.defaultStartTime = defaultStartTime; }

    public LocalTime getDefaultEndTime() { return defaultEndTime; }
    public void setDefaultEndTime(LocalTime defaultEndTime) { this.defaultEndTime = defaultEndTime; }

    public int getDefaultSessionMinutes() { return defaultSessionMinutes; }
    public void setDefaultSessionMinutes(int defaultSessionMinutes) { this.defaultSessionMinutes = defaultSessionMinutes; }

    public DayOfWeek getWeekStartDay() { return DayOfWeek.of(weekStartDay); }
    public void setWeekStartDay(DayOfWeek weekStartDay) { this.weekStartDay = weekStartDay.getValue(); }

    public DayOfWeek getCeremonyDay() { return DayOfWeek.of(ceremonyDay); }
    public void setCeremonyDay(DayOfWeek ceremonyDay) { this.ceremonyDay = ceremonyDay.getValue(); }
```

- [ ] **Step 4: Create PreferencesResponse DTO**

Create `backend/src/main/java/com/echel/planner/backend/auth/dto/PreferencesResponse.java`:

```java
package com.echel.planner.backend.auth.dto;

import com.echel.planner.backend.auth.AppUser;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record PreferencesResponse(
        String displayName,
        String timezone,
        LocalTime defaultStartTime,
        LocalTime defaultEndTime,
        int defaultSessionMinutes,
        DayOfWeek weekStartDay,
        DayOfWeek ceremonyDay
) {
    public static PreferencesResponse from(AppUser user) {
        return new PreferencesResponse(
                user.getDisplayName(),
                user.getTimezone(),
                user.getDefaultStartTime(),
                user.getDefaultEndTime(),
                user.getDefaultSessionMinutes(),
                user.getWeekStartDay(),
                user.getCeremonyDay()
        );
    }
}
```

- [ ] **Step 5: Create UpdatePreferencesRequest DTO**

Create `backend/src/main/java/com/echel/planner/backend/auth/dto/UpdatePreferencesRequest.java`:

```java
package com.echel.planner.backend.auth.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record UpdatePreferencesRequest(
        String displayName,
        String timezone,
        LocalTime defaultStartTime,
        LocalTime defaultEndTime,
        Integer defaultSessionMinutes,
        DayOfWeek weekStartDay,
        DayOfWeek ceremonyDay
) {}
```

- [ ] **Step 6: Create UserPreferencesService**

Create `backend/src/main/java/com/echel/planner/backend/auth/UserPreferencesService.java`:

```java
package com.echel.planner.backend.auth;

import com.echel.planner.backend.auth.dto.PreferencesResponse;
import com.echel.planner.backend.auth.dto.UpdatePreferencesRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.ZoneId;

@Service
@Transactional
public class UserPreferencesService {

    private final AppUserRepository userRepository;

    public UserPreferencesService(AppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PreferencesResponse getPreferences(AppUser user) {
        return PreferencesResponse.from(user);
    }

    public PreferencesResponse updatePreferences(AppUser user, UpdatePreferencesRequest request) {
        if (request.displayName() != null) {
            if (request.displayName().isBlank()) {
                throw new PreferencesValidationException("Preferred display name must not be blank");
            }
            user.setDisplayName(request.displayName().trim());
        }

        if (request.timezone() != null) {
            try {
                ZoneId.of(request.timezone());
            } catch (Exception e) {
                throw new PreferencesValidationException("Invalid timezone: " + request.timezone());
            }
            user.setTimezone(request.timezone());
        }

        // Resolve effective start/end for cross-validation
        LocalTime effectiveStart = request.defaultStartTime() != null
                ? request.defaultStartTime() : user.getDefaultStartTime();
        LocalTime effectiveEnd = request.defaultEndTime() != null
                ? request.defaultEndTime() : user.getDefaultEndTime();

        if (request.defaultStartTime() != null) {
            validateTimeAlignment(request.defaultStartTime());
            user.setDefaultStartTime(request.defaultStartTime());
        }

        if (request.defaultEndTime() != null) {
            validateTimeAlignment(request.defaultEndTime());
            user.setDefaultEndTime(request.defaultEndTime());
        }

        if (!effectiveStart.isBefore(effectiveEnd)) {
            throw new PreferencesValidationException("Start time must be before end time");
        }

        if (request.defaultSessionMinutes() != null) {
            int minutes = request.defaultSessionMinutes();
            if (minutes < 15 || minutes > 240 || minutes % 15 != 0) {
                throw new PreferencesValidationException(
                        "Session duration must be a multiple of 15 minutes (15-240)");
            }
            user.setDefaultSessionMinutes(minutes);
        }

        if (request.weekStartDay() != null) {
            user.setWeekStartDay(request.weekStartDay());
        }

        if (request.ceremonyDay() != null) {
            user.setCeremonyDay(request.ceremonyDay());
        }

        userRepository.save(user);
        return PreferencesResponse.from(user);
    }

    private void validateTimeAlignment(LocalTime time) {
        if (time.getMinute() % 15 != 0 || time.getSecond() != 0) {
            throw new PreferencesValidationException(
                    "Times must be aligned to 15-minute increments");
        }
    }

    static class PreferencesValidationException extends RuntimeException {
        PreferencesValidationException(String message) { super(message); }
    }
}
```

- [ ] **Step 7: Run tests to verify they pass**

Run: `cd backend && mvn test -pl . -Dtest=UserPreferencesServiceTest`
Expected: All 7 tests pass

- [ ] **Step 8: Commit**

```bash
git add backend/src/main/java/com/echel/planner/backend/auth/AppUser.java \
       backend/src/main/java/com/echel/planner/backend/auth/dto/PreferencesResponse.java \
       backend/src/main/java/com/echel/planner/backend/auth/dto/UpdatePreferencesRequest.java \
       backend/src/main/java/com/echel/planner/backend/auth/UserPreferencesService.java \
       backend/src/test/java/com/echel/planner/backend/auth/UserPreferencesServiceTest.java
git commit -m "feat: add UserPreferencesService with entity fields and DTOs"
```

---

### Task 3: Backend Controller and Exception Handler

**Goal:** Expose `GET` and `PATCH` endpoints for user preferences at `/api/v1/user/preferences` with proper error handling.

**Files:**
- Create: `backend/src/main/java/com/echel/planner/backend/auth/UserPreferencesController.java`
- Create: `backend/src/main/java/com/echel/planner/backend/auth/UserPreferencesExceptionHandler.java`
- Test: `backend/src/test/java/com/echel/planner/backend/auth/UserPreferencesControllerIntegrationTest.java`

**Acceptance Criteria:**
- [ ] GET returns 200 with all preference fields
- [ ] PATCH with valid partial payload returns 200 with updated values
- [ ] PATCH with invalid timezone returns 422
- [ ] PATCH with invalid session minutes returns 422
- [ ] Unauthenticated requests return 401

**Verify:** `cd backend && mvn test -pl . -Dtest=UserPreferencesControllerIntegrationTest` → all tests pass

**Steps:**

- [ ] **Step 1: Write the controller integration test**

Create `backend/src/test/java/com/echel/planner/backend/auth/UserPreferencesControllerIntegrationTest.java`:

```java
package com.echel.planner.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.echel.planner.backend.auth.dto.UpdatePreferencesRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserPreferencesController.class)
@Import({SecurityConfig.class, JwtService.class, JwtAuthFilter.class,
         UserPreferencesService.class, UserPreferencesExceptionHandler.class})
class UserPreferencesControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockBean private AppUserRepository userRepository;

    private AppUser testUser;
    private String accessToken;

    @BeforeEach
    void setUp() {
        testUser = new AppUser("alice@example.com",
                passwordEncoder.encode("secret123"), "Alice", "America/New_York");
        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);
        accessToken = jwtService.generateAccessToken("alice@example.com");
    }

    @Test
    void getPreferences_authenticated_returnsDefaults() throws Exception {
        mockMvc.perform(get("/api/v1/user/preferences")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Alice"))
                .andExpect(jsonPath("$.timezone").value("America/New_York"))
                .andExpect(jsonPath("$.defaultStartTime").value("08:00:00"))
                .andExpect(jsonPath("$.defaultEndTime").value("17:00:00"))
                .andExpect(jsonPath("$.defaultSessionMinutes").value(60))
                .andExpect(jsonPath("$.weekStartDay").value("MONDAY"))
                .andExpect(jsonPath("$.ceremonyDay").value("FRIDAY"));
    }

    @Test
    void getPreferences_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/user/preferences"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updatePreferences_partialUpdate_returns200() throws Exception {
        var request = new UpdatePreferencesRequest(
                null, null, null, null, null, null, DayOfWeek.THURSDAY
        );

        mockMvc.perform(patch("/api/v1/user/preferences")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ceremonyDay").value("THURSDAY"))
                .andExpect(jsonPath("$.displayName").value("Alice")); // unchanged
    }

    @Test
    void updatePreferences_invalidTimezone_returns422() throws Exception {
        var request = new UpdatePreferencesRequest(
                null, "Not/A/Zone", null, null, null, null, null
        );

        mockMvc.perform(patch("/api/v1/user/preferences")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void updatePreferences_invalidSessionMinutes_returns422() throws Exception {
        var request = new UpdatePreferencesRequest(
                null, null, null, null, 25, null, null
        );

        mockMvc.perform(patch("/api/v1/user/preferences")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd backend && mvn test -pl . -Dtest=UserPreferencesControllerIntegrationTest`
Expected: Compilation errors (controller/handler don't exist yet)

- [ ] **Step 3: Create the controller**

Create `backend/src/main/java/com/echel/planner/backend/auth/UserPreferencesController.java`:

```java
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
```

- [ ] **Step 4: Create the exception handler**

Create `backend/src/main/java/com/echel/planner/backend/auth/UserPreferencesExceptionHandler.java`:

```java
package com.echel.planner.backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class UserPreferencesExceptionHandler {

    @ExceptionHandler(UserPreferencesService.PreferencesValidationException.class)
    ProblemDetail handleValidation(UserPreferencesService.PreferencesValidationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `cd backend && mvn test -pl . -Dtest=UserPreferencesControllerIntegrationTest`
Expected: All 5 tests pass

- [ ] **Step 6: Run full backend test suite**

Run: `cd backend && mvn test`
Expected: All tests pass (existing tests not broken by new entity fields)

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/echel/planner/backend/auth/UserPreferencesController.java \
       backend/src/main/java/com/echel/planner/backend/auth/UserPreferencesExceptionHandler.java \
       backend/src/test/java/com/echel/planner/backend/auth/UserPreferencesControllerIntegrationTest.java
git commit -m "feat: add GET/PATCH endpoints for user preferences"
```

---

### Task 4: Frontend API Layer and Settings Page

**Goal:** Create the preferences API functions, TanStack Query hooks, and the settings page UI with form validation.

**Files:**
- Create: `frontend/src/api/preferences.js`
- Create: `frontend/src/pages/SettingsPage.jsx`
- Modify: `frontend/src/App.jsx` (add route)
- Modify: `frontend/src/layouts/AppLayout.jsx` (add nav link)

**Acceptance Criteria:**
- [ ] Settings page loads at `/settings` with current preference values
- [ ] Saving valid changes shows success feedback
- [ ] Saving invalid changes shows error feedback
- [ ] Settings link appears in sidebar navigation
- [ ] All form fields render with correct input types and constraints
- [ ] Timezone field is searchable

**Verify:** Start the dev server (`node dev.js start` from `frontend/`), navigate to `/settings`, verify form loads with defaults and save works end-to-end.

**Steps:**

- [ ] **Step 1: Create the API module**

Create `frontend/src/api/preferences.js`:

```javascript
import { authFetch, handleResponse } from './client'

const BASE = '/api/v1'

export async function getPreferences() {
  const res = await authFetch(`${BASE}/user/preferences`)
  return handleResponse(res)
}

export async function updatePreferences(data) {
  const res = await authFetch(`${BASE}/user/preferences`, {
    method: 'PATCH',
    body: JSON.stringify(data),
  })
  return handleResponse(res)
}
```

- [ ] **Step 2: Create the SettingsPage component**

Create `frontend/src/pages/SettingsPage.jsx`:

```jsx
import { useState, useEffect, useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import * as Label from '@radix-ui/react-label'
import { getPreferences, updatePreferences } from '@/api/preferences'

const DAYS_OF_WEEK = [
  { value: 'MONDAY', label: 'Monday' },
  { value: 'TUESDAY', label: 'Tuesday' },
  { value: 'WEDNESDAY', label: 'Wednesday' },
  { value: 'THURSDAY', label: 'Thursday' },
  { value: 'FRIDAY', label: 'Friday' },
  { value: 'SATURDAY', label: 'Saturday' },
  { value: 'SUNDAY', label: 'Sunday' },
]

const WEEK_START_OPTIONS = [
  { value: 'MONDAY', label: 'Monday' },
  { value: 'SUNDAY', label: 'Sunday' },
]

const SESSION_DURATIONS = Array.from({ length: 16 }, (_, i) => (i + 1) * 15)

function generateTimeOptions() {
  const options = []
  for (let h = 0; h < 24; h++) {
    for (let m = 0; m < 60; m += 15) {
      const value = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:00`
      const label = `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
      options.push({ value, label })
    }
  }
  return options
}

const TIME_OPTIONS = generateTimeOptions()

const TIMEZONE_LIST = Intl.supportedValuesOf('timeZone')

const inputClass =
  'w-full rounded-lg border border-edge px-3 py-2 text-ink-heading text-sm shadow-soft focus:outline-none focus:ring-2 focus:ring-edge-focus focus:border-edge-focus'

const selectClass =
  'w-full rounded-lg border border-edge px-3 py-2 text-ink-heading text-sm shadow-soft focus:outline-none focus:ring-2 focus:ring-edge-focus focus:border-edge-focus bg-surface'

function Field({ label, htmlFor, children }) {
  return (
    <div className="flex flex-col gap-1.5">
      <Label.Root htmlFor={htmlFor} className="text-sm font-medium text-ink-body">
        {label}
      </Label.Root>
      {children}
    </div>
  )
}

export function SettingsPage() {
  const queryClient = useQueryClient()
  const { data: prefs, isLoading } = useQuery({
    queryKey: ['preferences'],
    queryFn: getPreferences,
  })

  const [form, setForm] = useState(null)
  const [tzSearch, setTzSearch] = useState('')
  const [tzOpen, setTzOpen] = useState(false)
  const [success, setSuccess] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (prefs && !form) {
      setForm({
        displayName: prefs.displayName,
        timezone: prefs.timezone,
        defaultStartTime: prefs.defaultStartTime,
        defaultEndTime: prefs.defaultEndTime,
        defaultSessionMinutes: prefs.defaultSessionMinutes,
        weekStartDay: prefs.weekStartDay,
        ceremonyDay: prefs.ceremonyDay,
      })
    }
  }, [prefs, form])

  const filteredTimezones = useMemo(() => {
    if (!tzSearch) return TIMEZONE_LIST.slice(0, 20)
    const lower = tzSearch.toLowerCase()
    return TIMEZONE_LIST.filter(tz => tz.toLowerCase().includes(lower)).slice(0, 20)
  }, [tzSearch])

  const mutation = useMutation({
    mutationFn: updatePreferences,
    onSuccess: (data) => {
      queryClient.setQueryData(['preferences'], data)
      setSuccess(true)
      setError(null)
      setTimeout(() => setSuccess(false), 3000)
    },
    onError: (err) => {
      setError(err.message || 'Failed to save preferences')
      setSuccess(false)
    },
  })

  function handleSubmit(e) {
    e.preventDefault()
    setSuccess(false)
    setError(null)

    // Build partial payload — only send changed fields
    const payload = {}
    if (form.displayName !== prefs.displayName) payload.displayName = form.displayName
    if (form.timezone !== prefs.timezone) payload.timezone = form.timezone
    if (form.defaultStartTime !== prefs.defaultStartTime) payload.defaultStartTime = form.defaultStartTime
    if (form.defaultEndTime !== prefs.defaultEndTime) payload.defaultEndTime = form.defaultEndTime
    if (form.defaultSessionMinutes !== prefs.defaultSessionMinutes) payload.defaultSessionMinutes = form.defaultSessionMinutes
    if (form.weekStartDay !== prefs.weekStartDay) payload.weekStartDay = form.weekStartDay
    if (form.ceremonyDay !== prefs.ceremonyDay) payload.ceremonyDay = form.ceremonyDay

    if (Object.keys(payload).length === 0) {
      setSuccess(true)
      setTimeout(() => setSuccess(false), 3000)
      return
    }

    mutation.mutate(payload)
  }

  function update(field, value) {
    setForm(prev => ({ ...prev, [field]: value }))
  }

  if (isLoading || !form) {
    return (
      <div className="max-w-xl mx-auto px-6 py-10">
        <p className="text-ink-muted text-sm">Loading preferences...</p>
      </div>
    )
  }

  return (
    <div className="max-w-xl mx-auto px-6 py-10">
      <h1 className="text-2xl font-bold text-ink-heading mb-1">Settings</h1>
      <p className="text-sm text-ink-secondary mb-8">Customize how Planner works for you</p>

      <form onSubmit={handleSubmit} className="space-y-10">
        {/* Profile */}
        <section>
          <h2 className="text-lg font-semibold text-ink-heading mb-4">Profile</h2>
          <div className="space-y-4">
            <Field label="Preferred name" htmlFor="displayName">
              <input
                id="displayName"
                type="text"
                value={form.displayName}
                onChange={e => update('displayName', e.target.value)}
                className={inputClass}
                placeholder="What should we call you?"
              />
            </Field>
          </div>
        </section>

        {/* Schedule */}
        <section>
          <h2 className="text-lg font-semibold text-ink-heading mb-4">Schedule</h2>
          <div className="space-y-4">
            <Field label="Timezone" htmlFor="timezone">
              <div className="relative">
                <input
                  id="timezone"
                  type="text"
                  value={tzOpen ? tzSearch : form.timezone}
                  onChange={e => { setTzSearch(e.target.value); setTzOpen(true) }}
                  onFocus={() => { setTzSearch(''); setTzOpen(true) }}
                  onBlur={() => setTimeout(() => setTzOpen(false), 200)}
                  className={inputClass}
                  placeholder="Search timezones..."
                />
                {tzOpen && (
                  <ul className="absolute z-10 w-full mt-1 max-h-48 overflow-y-auto bg-surface-raised border border-edge rounded-lg shadow-modal">
                    {filteredTimezones.map(tz => (
                      <li key={tz}>
                        <button
                          type="button"
                          className="w-full text-left px-3 py-1.5 text-sm text-ink-heading hover:bg-surface-soft"
                          onMouseDown={() => { update('timezone', tz); setTzOpen(false) }}
                        >
                          {tz}
                        </button>
                      </li>
                    ))}
                    {filteredTimezones.length === 0 && (
                      <li className="px-3 py-2 text-sm text-ink-muted">No matches</li>
                    )}
                  </ul>
                )}
              </div>
            </Field>

            <div className="grid grid-cols-2 gap-4">
              <Field label="Day starts at" htmlFor="defaultStartTime">
                <select
                  id="defaultStartTime"
                  value={form.defaultStartTime}
                  onChange={e => update('defaultStartTime', e.target.value)}
                  className={selectClass}
                >
                  {TIME_OPTIONS.map(opt => (
                    <option key={opt.value} value={opt.value}>{opt.label}</option>
                  ))}
                </select>
              </Field>

              <Field label="Day ends at" htmlFor="defaultEndTime">
                <select
                  id="defaultEndTime"
                  value={form.defaultEndTime}
                  onChange={e => update('defaultEndTime', e.target.value)}
                  className={selectClass}
                >
                  {TIME_OPTIONS.map(opt => (
                    <option key={opt.value} value={opt.value}>{opt.label}</option>
                  ))}
                </select>
              </Field>
            </div>

            <Field label="Default session length" htmlFor="defaultSessionMinutes">
              <select
                id="defaultSessionMinutes"
                value={form.defaultSessionMinutes}
                onChange={e => update('defaultSessionMinutes', Number(e.target.value))}
                className={selectClass}
              >
                {SESSION_DURATIONS.map(m => (
                  <option key={m} value={m}>
                    {m >= 60 ? `${Math.floor(m / 60)}h${m % 60 ? ` ${m % 60}m` : ''}` : `${m}m`}
                  </option>
                ))}
              </select>
            </Field>
          </div>
        </section>

        {/* Rituals */}
        <section>
          <h2 className="text-lg font-semibold text-ink-heading mb-4">Rituals</h2>
          <div className="space-y-4">
            <Field label="Week starts on" htmlFor="weekStartDay">
              <select
                id="weekStartDay"
                value={form.weekStartDay}
                onChange={e => update('weekStartDay', e.target.value)}
                className={selectClass}
              >
                {WEEK_START_OPTIONS.map(opt => (
                  <option key={opt.value} value={opt.value}>{opt.label}</option>
                ))}
              </select>
            </Field>

            <Field label="End-of-week ceremony day" htmlFor="ceremonyDay">
              <select
                id="ceremonyDay"
                value={form.ceremonyDay}
                onChange={e => update('ceremonyDay', e.target.value)}
                className={selectClass}
              >
                {DAYS_OF_WEEK.map(opt => (
                  <option key={opt.value} value={opt.value}>{opt.label}</option>
                ))}
              </select>
            </Field>
          </div>
        </section>

        {/* Feedback and submit */}
        {error && (
          <p className="text-sm text-error font-medium" role="alert">{error}</p>
        )}
        {success && (
          <p className="text-sm text-emerald-600 font-medium" role="status">Preferences saved!</p>
        )}

        <button
          type="submit"
          disabled={mutation.isPending}
          className="py-2.5 px-6 bg-primary-500 hover:bg-primary-600 disabled:bg-primary-400
            text-white text-sm font-semibold rounded-lg shadow-soft
            focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-2
            transition-colors duration-150"
        >
          {mutation.isPending ? 'Saving...' : 'Save'}
        </button>
      </form>
    </div>
  )
}
```

- [ ] **Step 3: Add the route to App.jsx**

Modify `frontend/src/App.jsx`. Add import at top:

```javascript
import { SettingsPage } from '@/pages/SettingsPage'
```

Add route inside the `<Route element={<AppLayout />}>` block, after the `/start-month` route:

```jsx
<Route path="/settings" element={<SettingsPage />} />
```

- [ ] **Step 4: Add settings link to sidebar**

Modify `frontend/src/layouts/AppLayout.jsx`. In the bottom section of the sidebar (around line 249, inside the `px-4 py-4 border-t` div), add a settings link between QuickCapture and the user display name. Import `NavLink` is already imported.

Replace the bottom sidebar section:

```jsx
        <div className="px-4 py-4 border-t border-edge-subtle space-y-2">
          <QuickCapture />
          <NavLink
            to="/settings"
            className={({ isActive }) =>
              [
                'flex items-center gap-2 px-3 py-2 rounded-md text-sm transition-colors duration-100 focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-1',
                isActive
                  ? 'bg-surface-accent text-primary-700 font-medium'
                  : 'text-ink-secondary hover:bg-surface-soft hover:text-ink-heading',
              ].join(' ')
            }
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none"
              stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
              aria-hidden="true">
              <circle cx="12" cy="12" r="3" />
              <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" />
            </svg>
            Settings
          </NavLink>
          <p className="text-xs text-ink-muted truncate" title={displayName}>
            {displayName}
          </p>
          <button
            onClick={logout}
            className="w-full text-left px-3 py-2 rounded-md text-sm text-ink-secondary hover:bg-surface-soft hover:text-ink-heading transition-colors duration-100 focus:outline-none focus:ring-2 focus:ring-edge-focus focus:ring-offset-1"
          >
            Log out
          </button>
        </div>
```

- [ ] **Step 5: Verify visually**

Run: `cd frontend && node dev.js start` (or `npm run dev`)
Navigate to `/settings`, verify:
- Page loads with default values populated
- Timezone search filters correctly
- Save works and shows success message
- Invalid changes (e.g., blank name) show error from backend

- [ ] **Step 6: Commit**

```bash
git add frontend/src/api/preferences.js \
       frontend/src/pages/SettingsPage.jsx \
       frontend/src/App.jsx \
       frontend/src/layouts/AppLayout.jsx
git commit -m "feat: add settings page with preferences form"
```

---

### Task 5: Wire Preferences into Schedule Service

**Goal:** Replace hardcoded schedule defaults (start hour 8, end hour 17) with user preferences, so the schedule planner respects the user's configured work hours.

**Files:**
- Modify: `backend/src/main/java/com/echel/planner/backend/schedule/ScheduleService.java`
- Modify: `backend/src/test/java/com/echel/planner/backend/schedule/ScheduleServiceTest.java` (if exists, otherwise create)

**Acceptance Criteria:**
- [ ] `savePlan()` uses user's `defaultStartTime` and `defaultEndTime` when request doesn't specify hours
- [ ] Explicit `startHour`/`endHour` in request still override user preferences
- [ ] Existing tests still pass

**Verify:** `cd backend && mvn test` → all tests pass

**Steps:**

- [ ] **Step 1: Modify ScheduleService.savePlan()**

In `backend/src/main/java/com/echel/planner/backend/schedule/ScheduleService.java`, replace the default logic at lines 50-51:

Replace:
```java
    private static final int DEFAULT_START_HOUR = 8;
    private static final int DEFAULT_END_HOUR = 17;
```

with:
```java
    // Removed: hardcoded defaults. Now sourced from user preferences.
```

Replace the defaulting logic in `savePlan()` (lines 50-51):
```java
        int startHour = request.startHour() != null ? request.startHour() : DEFAULT_START_HOUR;
        int endHour = request.endHour() != null ? request.endHour() : DEFAULT_END_HOUR;
```

with:
```java
        int startHour = request.startHour() != null
                ? request.startHour() : user.getDefaultStartTime().getHour();
        int endHour = request.endHour() != null
                ? request.endHour() : user.getDefaultEndTime().getHour();
```

- [ ] **Step 2: Run full test suite**

Run: `cd backend && mvn test`
Expected: All tests pass

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/echel/planner/backend/schedule/ScheduleService.java
git commit -m "feat: use user preferences for schedule default hours"
```

---

### Task 6: E2E Test for Settings Page

**Goal:** Add a Playwright E2E test that verifies the settings page loads, displays preferences, and handles save/error flows.

**Files:**
- Create: `e2e/tests/settings.spec.ts`

**Acceptance Criteria:**
- [ ] Test verifies settings page loads with preference values
- [ ] Test verifies saving changes shows success feedback
- [ ] Test verifies validation errors display correctly
- [ ] All API calls are mocked (no backend required)

**Verify:** `cd e2e && npx playwright test settings.spec.ts` → all tests pass

**Steps:**

- [ ] **Step 1: Create the E2E test**

Create `e2e/tests/settings.spec.ts`:

```typescript
import { test, expect } from '@playwright/test'

const AUTH_RESPONSE = {
  accessToken: 'test-token',
  user: { id: 'u1', displayName: 'Test User', timezone: 'America/New_York' },
}

const PREFERENCES = {
  displayName: 'Test User',
  timezone: 'America/New_York',
  defaultStartTime: '09:00:00',
  defaultEndTime: '17:00:00',
  defaultSessionMinutes: 60,
  weekStartDay: 'MONDAY',
  ceremonyDay: 'FRIDAY',
}

async function mockSettingsPage(page: any) {
  await page.route('**/api/v1/auth/refresh', route =>
    route.fulfill({ json: AUTH_RESPONSE })
  )
  await page.route('**/api/v1/deferred', route =>
    route.fulfill({ json: [] })
  )
  await page.route('**/api/v1/user/preferences', route => {
    if (route.request().method() === 'GET') {
      route.fulfill({ json: PREFERENCES })
    } else if (route.request().method() === 'PATCH') {
      const body = route.request().postDataJSON()
      route.fulfill({ json: { ...PREFERENCES, ...body } })
    }
  })
}

test.describe('Settings page', () => {
  test('loads and displays current preferences', async ({ page }) => {
    await mockSettingsPage(page)
    await page.goto('/settings')

    await expect(page.getByRole('heading', { name: 'Settings' })).toBeVisible()
    await expect(page.locator('#displayName')).toHaveValue('Test User')
    await expect(page.locator('#weekStartDay')).toHaveValue('MONDAY')
    await expect(page.locator('#ceremonyDay')).toHaveValue('FRIDAY')
  })

  test('saving changes shows success message', async ({ page }) => {
    await mockSettingsPage(page)
    await page.goto('/settings')

    await page.locator('#ceremonyDay').selectOption('THURSDAY')
    await page.getByRole('button', { name: 'Save' }).click()

    await expect(page.getByText('Preferences saved!')).toBeVisible()
  })

  test('settings link visible in sidebar', async ({ page }) => {
    await mockSettingsPage(page)
    await page.goto('/')

    // Mock dashboard endpoints
    await page.route('**/api/v1/stats/**', route =>
      route.fulfill({ json: {} })
    )
    await page.route('**/api/v1/schedule/today', route =>
      route.fulfill({ json: [] })
    )
    await page.route('**/api/v1/tasks/**', route =>
      route.fulfill({ json: [] })
    )
    await page.route('**/api/v1/events/**', route =>
      route.fulfill({ json: [] })
    )

    const settingsLink = page.getByRole('link', { name: 'Settings' })
    await expect(settingsLink).toBeVisible()
  })

  test('backend validation error displays on page', async ({ page }) => {
    await page.route('**/api/v1/auth/refresh', route =>
      route.fulfill({ json: AUTH_RESPONSE })
    )
    await page.route('**/api/v1/deferred', route =>
      route.fulfill({ json: [] })
    )
    await page.route('**/api/v1/user/preferences', route => {
      if (route.request().method() === 'GET') {
        route.fulfill({ json: PREFERENCES })
      } else if (route.request().method() === 'PATCH') {
        route.fulfill({
          status: 422,
          json: { detail: 'Start time must be before end time' },
        })
      }
    })

    await page.goto('/settings')
    await page.locator('#ceremonyDay').selectOption('THURSDAY')
    await page.getByRole('button', { name: 'Save' }).click()

    await expect(page.getByRole('alert')).toBeVisible()
  })
})
```

- [ ] **Step 2: Run the E2E test**

Run: `cd e2e && npx playwright test settings.spec.ts`
Expected: All 4 tests pass

- [ ] **Step 3: Commit**

```bash
git add e2e/tests/settings.spec.ts
git commit -m "test: add E2E tests for settings page"
```

---

### Task 7: Wire Week Start Day into Frontend Ritual Components

**Goal:** Replace the hardcoded Monday week start in `TaskSchedulePhase` with the user's configured `weekStartDay` preference.

**Files:**
- Modify: `frontend/src/components/ritual/TaskSchedulePhase.jsx`

**Acceptance Criteria:**
- [ ] Start Week calendar renders with the user's configured week start day
- [ ] Day labels shift correctly (e.g., Sunday start shows Sun-Mon-Tue-...-Sat)
- [ ] Deferral "next week" calculation uses the configured week start

**Verify:** Start the dev server, navigate to `/start-week`, verify the calendar starts on the configured day. Change the setting and reload to confirm it updates.

**Steps:**

- [ ] **Step 1: Add preferences query to TaskSchedulePhase**

In `frontend/src/components/ritual/TaskSchedulePhase.jsx`, add import for the preferences API:

```javascript
import { getPreferences } from '@/api/preferences'
```

Add a `useQuery` call near the top of the component:

```javascript
const { data: prefs } = useQuery({
  queryKey: ['preferences'],
  queryFn: getPreferences,
})
const weekStartDay = prefs?.weekStartDay || 'MONDAY'
```

- [ ] **Step 2: Parameterize getDaysInWeek**

Replace the `getDaysInWeek()` function (currently hardcoded to Monday) with a version that accepts the week start day:

```javascript
function getDaysInWeek(weekStartDay) {
  const dayMap = { MONDAY: 1, TUESDAY: 2, WEDNESDAY: 3, THURSDAY: 4, FRIDAY: 5, SATURDAY: 6, SUNDAY: 0 }
  const nameMap = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
  const startDayJs = dayMap[weekStartDay] ?? 1 // JS: 0=Sun, 1=Mon, ...

  const today = new Date()
  const currentDayJs = today.getDay()
  const diff = (currentDayJs - startDayJs + 7) % 7
  const weekStart = new Date(today)
  weekStart.setDate(weekStart.getDate() - diff)

  const days = []
  for (let i = 0; i < 7; i++) {
    const d = new Date(weekStart)
    d.setDate(d.getDate() + i)
    days.push({
      label: nameMap[d.getDay()],
      dateLabel: `${d.getMonth() + 1}/${d.getDate()}`,
      iso: d.toISOString().split('T')[0],
    })
  }
  return days
}
```

Update the call site from `getDaysInWeek()` to `getDaysInWeek(weekStartDay)`.

- [ ] **Step 3: Verify visually**

Navigate to `/start-week` and confirm the calendar starts on the configured day.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/components/ritual/TaskSchedulePhase.jsx
git commit -m "feat: use user preference for week start day in schedule phase"
```
