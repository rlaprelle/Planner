# Slice 6: Polish & Stats Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers-extended-cc:subagent-driven-development (recommended) or superpowers-extended-cc:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add weekly stats visibility to the dashboard and intelligent celebration of accomplishments in the evening reflection.

**Architecture:** New `GET /api/v1/stats/weekly-summary` endpoint returns rolling 7-day stats. Celebration logic is added to the existing dashboard endpoint response. Frontend adds a banner card to DashboardPage and a callout section to EndDayPage Phase2.

**Tech Stack:** Spring Boot (backend), React + TanStack Query (frontend), existing auth/API patterns.

---

### Task 1: Weekly Summary Backend

**Goal:** Add repository queries, DTO, service method, and controller endpoint for `GET /api/v1/stats/weekly-summary`.

**Files:**
- Create: `backend/src/main/java/com/echel/planner/backend/stats/dto/WeeklySummaryResponse.java`
- Modify: `backend/src/main/java/com/echel/planner/backend/task/TaskRepository.java`
- Modify: `backend/src/main/java/com/echel/planner/backend/reflection/DailyReflectionRepository.java`
- Modify: `backend/src/main/java/com/echel/planner/backend/stats/StatsService.java`
- Modify: `backend/src/main/java/com/echel/planner/backend/stats/StatsController.java`
- Test: `backend/src/test/java/com/echel/planner/backend/stats/StatsControllerWeeklySummaryTest.java`

**Acceptance Criteria:**
- [ ] `GET /api/v1/stats/weekly-summary` returns correct stats for rolling 7-day window
- [ ] Trend calculation returns "improving", "declining", "steady", or null
- [ ] Empty state returns `hasActivity: false` when no completions exist
- [ ] Endpoint requires authentication

**Verify:** `cd backend && mvn test -Dtest=StatsControllerWeeklySummaryTest` → all tests pass

**Steps:**

- [ ] **Step 1: Create WeeklySummaryResponse DTO**

Create `backend/src/main/java/com/echel/planner/backend/stats/dto/WeeklySummaryResponse.java`:

```java
package com.echel.planner.backend.stats.dto;

/**
 * Rolling 7-day stats summary for the dashboard banner.
 */
public record WeeklySummaryResponse(
        int tasksCompleted,
        int totalPoints,
        int totalFocusMinutes,
        int streakDays,
        String energyTrend,
        String moodTrend,
        boolean hasActivity
) {}
```

- [ ] **Step 2: Add repository queries**

Add to `TaskRepository.java` (after the existing `findCompletedTodayForUser` method):

```java
@Query("""
        SELECT t FROM Task t
        WHERE t.user.id = :userId
          AND t.status = com.echel.planner.backend.task.TaskStatus.DONE
          AND t.completedAt >= :start
          AND t.completedAt < :end
          AND t.archivedAt IS NULL
        """)
List<Task> findCompletedInRange(@Param("userId") UUID userId,
                                @Param("start") Instant start,
                                @Param("end") Instant end);
```

Add to `DailyReflectionRepository.java` (after the existing `findFinalizedDatesDesc` method):

```java
@Query("""
        SELECT r FROM DailyReflection r
        WHERE r.user = :user
          AND r.isFinalized = true
          AND r.reflectionDate >= :start
          AND r.reflectionDate <= :end
        ORDER BY r.reflectionDate ASC
        """)
List<DailyReflection> findFinalizedInDateRange(@Param("user") AppUser user,
                                                @Param("start") LocalDate start,
                                                @Param("end") LocalDate end);
```

Note: `DailyReflectionRepository` will need the `DailyReflection` import — it's already in the same package, so no import needed.

- [ ] **Step 3: Add getWeeklySummary method to StatsService**

Add these imports to `StatsService.java`:

```java
import com.echel.planner.backend.reflection.DailyReflection;
import com.echel.planner.backend.stats.dto.WeeklySummaryResponse;
import java.time.Instant;
import java.time.LocalTime;
```

Add this method to `StatsService.java` after the `getDashboard` method:

```java
/**
 * Computes rolling 7-day stats: tasks completed, points, focus minutes,
 * streak, and mood/energy trends.
 */
public WeeklySummaryResponse getWeeklySummary(AppUser user) {
    ZoneId zone = ZoneId.of(user.getTimezone());
    LocalDate today = LocalDate.now(zone);
    LocalDate weekStart = today.minusDays(6);

    Instant rangeStart = weekStart.atStartOfDay(zone).toInstant();
    Instant rangeEnd = today.plusDays(1).atStartOfDay(zone).toInstant();

    List<Task> completed = taskRepository.findCompletedInRange(user.getId(), rangeStart, rangeEnd);

    int tasksCompleted = completed.size();
    int totalPoints = completed.stream()
            .mapToInt(t -> t.getPointsEstimate() != null ? t.getPointsEstimate() : 0)
            .sum();
    int totalFocusMinutes = completed.stream()
            .mapToInt(t -> t.getActualMinutes() != null ? t.getActualMinutes() : 0)
            .sum();

    int streak = computeStreak(user, true);

    List<DailyReflection> reflections = reflectionRepository.findFinalizedInDateRange(
            user, weekStart, today);

    String energyTrend = computeTrend(reflections, DailyReflection::getEnergyRating);
    String moodTrend = computeTrend(reflections, DailyReflection::getMoodRating);

    boolean hasActivity = tasksCompleted > 0 || totalFocusMinutes > 0;

    return new WeeklySummaryResponse(
            tasksCompleted, totalPoints, totalFocusMinutes,
            streak, energyTrend, moodTrend, hasActivity);
}

private String computeTrend(List<DailyReflection> reflections,
                             java.util.function.ToIntFunction<DailyReflection> ratingExtractor) {
    if (reflections.size() < 2) {
        return null;
    }
    int mid = reflections.size() / 2;
    double earlier = reflections.subList(0, mid).stream()
            .mapToInt(ratingExtractor)
            .average()
            .orElse(0);
    double later = reflections.subList(mid, reflections.size()).stream()
            .mapToInt(ratingExtractor)
            .average()
            .orElse(0);

    double diff = later - earlier;
    if (diff > 0.5) return "improving";
    if (diff < -0.5) return "declining";
    return "steady";
}
```

- [ ] **Step 4: Add controller endpoint**

Add this import to `StatsController.java`:

```java
import com.echel.planner.backend.stats.dto.WeeklySummaryResponse;
```

Add this method to `StatsController.java` after the `dashboard` method:

```java
@GetMapping("/weekly-summary")
public ResponseEntity<WeeklySummaryResponse> weeklySummary(@AuthenticationPrincipal AppUser user) {
    return ResponseEntity.ok(statsService.getWeeklySummary(user));
}
```

- [ ] **Step 5: Write integration test**

Create `backend/src/test/java/com/echel/planner/backend/stats/StatsControllerWeeklySummaryTest.java`:

```java
package com.echel.planner.backend.stats;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.project.Project;
import com.echel.planner.backend.project.ProjectRepository;
import com.echel.planner.backend.reflection.DailyReflection;
import com.echel.planner.backend.reflection.DailyReflectionRepository;
import com.echel.planner.backend.task.Task;
import com.echel.planner.backend.task.TaskRepository;
import com.echel.planner.backend.task.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StatsControllerWeeklySummaryTest {

    @Autowired MockMvc mockMvc;
    @Autowired AppUserRepository userRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired TaskRepository taskRepository;
    @Autowired DailyReflectionRepository reflectionRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String token;
    private AppUser user;
    private Project project;

    @BeforeEach
    void setUp() throws Exception {
        user = new AppUser();
        user.setEmail("weekly-test@example.com");
        user.setPasswordHash(passwordEncoder.encode("password"));
        user.setTimezone("UTC");
        user = userRepository.save(user);

        project = new Project(user, "Test Project");
        project = projectRepository.save(project);

        // Login to get token
        String loginResponse = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"weekly-test@example.com\",\"password\":\"password\"}")
        ).andReturn().getResponse().getContentAsString();

        token = com.jayway.jsonpath.JsonPath.read(loginResponse, "$.accessToken");
    }

    @Test
    void returnsEmptyStateWhenNoActivity() throws Exception {
        mockMvc.perform(get("/api/v1/stats/weekly-summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasksCompleted").value(0))
                .andExpect(jsonPath("$.totalPoints").value(0))
                .andExpect(jsonPath("$.totalFocusMinutes").value(0))
                .andExpect(jsonPath("$.hasActivity").value(false));
    }

    @Test
    void countsCompletedTasksInRollingWindow() throws Exception {
        // Task completed today
        Task t1 = new Task(user, project, "Done today");
        t1.setStatus(TaskStatus.DONE);
        t1.setCompletedAt(Instant.now());
        t1.setPointsEstimate((short) 5);
        t1.setActualMinutes(90);
        taskRepository.save(t1);

        // Task completed 3 days ago
        Task t2 = new Task(user, project, "Done 3 days ago");
        t2.setStatus(TaskStatus.DONE);
        t2.setCompletedAt(Instant.now().minus(3, ChronoUnit.DAYS));
        t2.setPointsEstimate((short) 3);
        t2.setActualMinutes(60);
        taskRepository.save(t2);

        // Task completed 10 days ago — outside window
        Task t3 = new Task(user, project, "Done 10 days ago");
        t3.setStatus(TaskStatus.DONE);
        t3.setCompletedAt(Instant.now().minus(10, ChronoUnit.DAYS));
        t3.setPointsEstimate((short) 8);
        taskRepository.save(t3);

        mockMvc.perform(get("/api/v1/stats/weekly-summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasksCompleted").value(2))
                .andExpect(jsonPath("$.totalPoints").value(8))
                .andExpect(jsonPath("$.totalFocusMinutes").value(150))
                .andExpect(jsonPath("$.hasActivity").value(true));
    }

    @Test
    void computesMoodAndEnergyTrends() throws Exception {
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));

        // Older reflections — low ratings
        saveReflection(today.minusDays(5), (short) 2, (short) 2);
        saveReflection(today.minusDays(4), (short) 2, (short) 1);

        // Recent reflections — high ratings
        saveReflection(today.minusDays(1), (short) 4, (short) 5);
        saveReflection(today, (short) 5, (short) 4);

        mockMvc.perform(get("/api/v1/stats/weekly-summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.energyTrend").value("improving"))
                .andExpect(jsonPath("$.moodTrend").value("improving"));
    }

    @Test
    void returnsNullTrendsWithInsufficientData() throws Exception {
        // Only one reflection — not enough for trend
        saveReflection(LocalDate.now(ZoneId.of("UTC")), (short) 3, (short) 3);

        mockMvc.perform(get("/api/v1/stats/weekly-summary")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.energyTrend").doesNotExist())
                .andExpect(jsonPath("$.moodTrend").doesNotExist());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/stats/weekly-summary"))
                .andExpect(status().isUnauthorized());
    }

    private void saveReflection(LocalDate date, short energy, short mood) {
        DailyReflection r = new DailyReflection();
        r.setUser(user);
        r.setReflectionDate(date);
        r.setEnergyRating(energy);
        r.setMoodRating(mood);
        r.setFinalized(true);
        reflectionRepository.save(r);
    }
}
```

- [ ] **Step 6: Run tests and commit**

Run: `cd backend && mvn test -Dtest=StatsControllerWeeklySummaryTest`
Expected: All 5 tests pass.

```bash
git add backend/src/main/java/com/echel/planner/backend/stats/dto/WeeklySummaryResponse.java \
  backend/src/main/java/com/echel/planner/backend/task/TaskRepository.java \
  backend/src/main/java/com/echel/planner/backend/reflection/DailyReflectionRepository.java \
  backend/src/main/java/com/echel/planner/backend/stats/StatsService.java \
  backend/src/main/java/com/echel/planner/backend/stats/StatsController.java \
  backend/src/test/java/com/echel/planner/backend/stats/StatsControllerWeeklySummaryTest.java
git commit -m "feat: add GET /api/v1/stats/weekly-summary endpoint"
```

---

### Task 2: Celebration Logic Backend

**Goal:** Add celebration task identification to the existing dashboard endpoint response.

**Files:**
- Modify: `backend/src/main/java/com/echel/planner/backend/stats/dto/DashboardResponse.java`
- Modify: `backend/src/main/java/com/echel/planner/backend/stats/StatsService.java`
- Test: `backend/src/test/java/com/echel/planner/backend/stats/StatsControllerCelebrationTest.java`

**Acceptance Criteria:**
- [ ] `GET /api/v1/stats/dashboard` includes `celebrationTasks` array
- [ ] Tasks with `points_estimate >= 5` qualify
- [ ] Tasks with `actual_minutes >= 120` qualify
- [ ] Tasks older than 7 days qualify
- [ ] Max 3 celebration tasks returned
- [ ] Each has a human-readable reason string
- [ ] Empty array when no tasks qualify

**Verify:** `cd backend && mvn test -Dtest=StatsControllerCelebrationTest` → all tests pass

**Steps:**

- [ ] **Step 1: Add CelebrationTask to DashboardResponse**

Update `DashboardResponse.java` to add the nested record and new field:

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
        int deferredItemCount,
        List<CelebrationTask> celebrationTasks
) {
    public record DeadlineSummary(
            UUID taskId,
            String taskTitle,
            String projectName,
            String projectColor,
            LocalDate dueDate,
            String deadlineGroup
    ) {}

    public record CelebrationTask(
            UUID taskId,
            String taskTitle,
            String projectName,
            String reason
    ) {}
}
```

- [ ] **Step 2: Add celebration logic to StatsService**

Add this import to `StatsService.java`:

```java
import com.echel.planner.backend.stats.dto.DashboardResponse.CelebrationTask;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
```

Update the `getDashboard` method — replace the final `return` statement with:

```java
        List<CelebrationTask> celebrations = buildCelebrations(user, zone);

        return new DashboardResponse(
                (int) blockCount,
                (int) completedCount,
                streak,
                deadlines,
                (int) deferredCount,
                celebrations);
```

Add this private method after `computeDeadlineGroup`:

```java
/**
 * Identifies today's completed tasks worth celebrating:
 * high points (>= 5), high time (>= 120 min), or long-running (>= 7 days old).
 */
private List<CelebrationTask> buildCelebrations(AppUser user, ZoneId zone) {
    LocalDate today = LocalDate.now(zone);
    Instant startOfDay = today.atStartOfDay(zone).toInstant();
    List<Task> completed = taskRepository.findCompletedTodayForUser(user.getId(), startOfDay);

    List<CelebrationTask> celebrations = new ArrayList<>();
    for (Task t : completed) {
        String reason = pickCelebrationReason(t);
        if (reason != null) {
            celebrations.add(new CelebrationTask(
                    t.getId(),
                    t.getTitle(),
                    t.getProject().getName(),
                    reason));
        }
    }

    celebrations.sort(Comparator
            .comparingInt((CelebrationTask c) -> {
                // Sort by reason priority: time > points > age
                if (c.reason().contains("hour") || c.reason().contains("minute")) return 0;
                if (c.reason().contains("complexity")) return 1;
                return 2;
            }));

    return celebrations.size() > 3 ? celebrations.subList(0, 3) : celebrations;
}

private String pickCelebrationReason(Task task) {
    // Priority: time invested > points > age
    if (task.getActualMinutes() != null && task.getActualMinutes() >= 120) {
        int hours = task.getActualMinutes() / 60;
        int mins = task.getActualMinutes() % 60;
        if (mins == 0) {
            return hours + (hours == 1 ? " hour" : " hours") + " of focused work";
        }
        return hours + "h " + mins + "m of focused work";
    }
    if (task.getPointsEstimate() != null && task.getPointsEstimate() >= 5) {
        return "High complexity task";
    }
    if (task.getCreatedAt() != null && task.getCompletedAt() != null) {
        long days = ChronoUnit.DAYS.between(task.getCreatedAt(), task.getCompletedAt());
        if (days >= 7) {
            return "On your list for " + days + " days";
        }
    }
    return null;
}
```

- [ ] **Step 3: Write integration test**

Create `backend/src/test/java/com/echel/planner/backend/stats/StatsControllerCelebrationTest.java`:

```java
package com.echel.planner.backend.stats;

import com.echel.planner.backend.auth.AppUser;
import com.echel.planner.backend.auth.AppUserRepository;
import com.echel.planner.backend.project.Project;
import com.echel.planner.backend.project.ProjectRepository;
import com.echel.planner.backend.task.Task;
import com.echel.planner.backend.task.TaskRepository;
import com.echel.planner.backend.task.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StatsControllerCelebrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired AppUserRepository userRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired TaskRepository taskRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String token;
    private AppUser user;
    private Project project;

    @BeforeEach
    void setUp() throws Exception {
        user = new AppUser();
        user.setEmail("celebration-test@example.com");
        user.setPasswordHash(passwordEncoder.encode("password"));
        user.setTimezone("UTC");
        user = userRepository.save(user);

        project = new Project(user, "Test Project");
        project = projectRepository.save(project);

        String loginResponse = mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"celebration-test@example.com\",\"password\":\"password\"}")
        ).andReturn().getResponse().getContentAsString();

        token = com.jayway.jsonpath.JsonPath.read(loginResponse, "$.accessToken");
    }

    @Test
    void returnsEmptyCelebrationsWhenNoTasksQualify() throws Exception {
        Task t = new Task(user, project, "Simple task");
        t.setStatus(TaskStatus.DONE);
        t.setCompletedAt(Instant.now());
        t.setPointsEstimate((short) 2);
        taskRepository.save(t);

        mockMvc.perform(get("/api/v1/stats/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.celebrationTasks").isArray())
                .andExpect(jsonPath("$.celebrationTasks").isEmpty());
    }

    @Test
    void celebratesHighPointsTask() throws Exception {
        Task t = new Task(user, project, "Big task");
        t.setStatus(TaskStatus.DONE);
        t.setCompletedAt(Instant.now());
        t.setPointsEstimate((short) 5);
        taskRepository.save(t);

        mockMvc.perform(get("/api/v1/stats/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.celebrationTasks[0].taskTitle").value("Big task"))
                .andExpect(jsonPath("$.celebrationTasks[0].reason").value("High complexity task"));
    }

    @Test
    void celebratesHighTimeTask() throws Exception {
        Task t = new Task(user, project, "Long session");
        t.setStatus(TaskStatus.DONE);
        t.setCompletedAt(Instant.now());
        t.setActualMinutes(150);
        taskRepository.save(t);

        mockMvc.perform(get("/api/v1/stats/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.celebrationTasks[0].taskTitle").value("Long session"))
                .andExpect(jsonPath("$.celebrationTasks[0].reason").value("2h 30m of focused work"));
    }

    @Test
    void capsAtThreeCelebrations() throws Exception {
        for (int i = 1; i <= 5; i++) {
            Task t = new Task(user, project, "Task " + i);
            t.setStatus(TaskStatus.DONE);
            t.setCompletedAt(Instant.now());
            t.setPointsEstimate((short) 5);
            taskRepository.save(t);
        }

        mockMvc.perform(get("/api/v1/stats/dashboard")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.celebrationTasks.length()").value(3));
    }
}
```

- [ ] **Step 4: Run tests and commit**

Run: `cd backend && mvn test -Dtest=StatsControllerCelebrationTest`
Expected: All 4 tests pass.

Also verify existing dashboard tests still pass: `cd backend && mvn test -Dtest=StatsControllerTest`

```bash
git add backend/src/main/java/com/echel/planner/backend/stats/dto/DashboardResponse.java \
  backend/src/main/java/com/echel/planner/backend/stats/StatsService.java \
  backend/src/test/java/com/echel/planner/backend/stats/StatsControllerCelebrationTest.java
git commit -m "feat: add celebration logic to dashboard endpoint"
```

---

### Task 3: Dashboard Weekly Summary Banner

**Goal:** Add a `getWeeklySummary()` API function and a full-width banner card above the existing dashboard grid.

**Files:**
- Modify: `frontend/src/api/dashboard.js`
- Modify: `frontend/src/pages/DashboardPage.jsx`

**Acceptance Criteria:**
- [ ] Banner displays above the 2x2 card grid
- [ ] Shows tasks completed, points, focus time, streak, and trends
- [ ] Null trends are omitted (not shown as "null")
- [ ] Empty state shows "No activity yet this week."
- [ ] Loading state handled gracefully

**Verify:** Start frontend (`cd frontend && npm run dev`), navigate to dashboard, confirm banner renders with data from backend.

**Steps:**

- [ ] **Step 1: Add API function**

Update `frontend/src/api/dashboard.js`:

```javascript
import { authFetch, handleResponse } from './client'

export async function getDashboard() {
  const res = await authFetch('/api/v1/stats/dashboard')
  return handleResponse(res)
}

export async function getWeeklySummary() {
  const res = await authFetch('/api/v1/stats/weekly-summary')
  return handleResponse(res)
}
```

- [ ] **Step 2: Add banner to DashboardPage**

Add import for `getWeeklySummary` at the top of `DashboardPage.jsx`:

```javascript
import { getDashboard, getWeeklySummary } from '@/api/dashboard'
```

Add the weekly summary query inside `DashboardPage`, after the existing `todayBlocks` query:

```javascript
  const { data: weeklySummary } = useQuery({
    queryKey: ['stats', 'weekly-summary'],
    queryFn: getWeeklySummary,
  })
```

Add the `WeeklyBanner` component above `DashboardPage` (after the `CardLabel` component, before the `DEADLINE_BADGE` constant):

```jsx
function formatFocusTime(minutes) {
  if (!minutes || minutes === 0) return '0m'
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  if (h === 0) return `${m}m`
  if (m === 0) return `${h}h`
  return `${h}h ${m}m`
}

function WeeklyBanner({ summary }) {
  if (!summary) return null

  if (!summary.hasActivity) {
    return (
      <Card className="mb-4">
        <CardLabel>Your Week</CardLabel>
        <p className="text-sm text-gray-500">No activity yet this week.</p>
      </Card>
    )
  }

  const trendParts = []
  if (summary.streakDays > 0) {
    trendParts.push(`${summary.streakDays}-day streak`)
  }
  if (summary.energyTrend) {
    trendParts.push(`Energy: ${summary.energyTrend}`)
  }
  if (summary.moodTrend) {
    trendParts.push(`Mood: ${summary.moodTrend}`)
  }

  return (
    <Card className="mb-4">
      <CardLabel>Your Week</CardLabel>
      <p className="text-lg font-semibold text-gray-900">
        {summary.tasksCompleted} {summary.tasksCompleted === 1 ? 'task' : 'tasks'} completed
        <span className="text-gray-400 font-normal mx-2">·</span>
        {summary.totalPoints} {summary.totalPoints === 1 ? 'point' : 'points'}
        <span className="text-gray-400 font-normal mx-2">·</span>
        {formatFocusTime(summary.totalFocusMinutes)} focused
      </p>
      {trendParts.length > 0 && (
        <p className="mt-1 text-sm text-gray-500">{trendParts.join(' · ')}</p>
      )}
    </Card>
  )
}
```

Add the banner in the JSX, right after the `<h1>` heading and before the `<div className="grid ...">`:

```jsx
      <WeeklyBanner summary={weeklySummary} />
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/api/dashboard.js frontend/src/pages/DashboardPage.jsx
git commit -m "feat: add weekly summary banner to dashboard"
```

---

### Task 4: Evening Reflection Celebration Callout

**Goal:** Show a "Notable today" callout in the End Day reflection phase for celebration-worthy tasks.

**Files:**
- Modify: `frontend/src/pages/EndDayPage.jsx`

**Acceptance Criteria:**
- [ ] Callout appears above the reflection form in Phase 2
- [ ] Shows task title, project name, and reason for each celebration task
- [ ] Does not render when no tasks qualify
- [ ] Max 3 items displayed
- [ ] Styled with soft lavender background

**Verify:** Start frontend, navigate to End Day, process inbox (or skip if empty), confirm callout renders in Phase 2 when celebration-worthy tasks exist.

**Steps:**

- [ ] **Step 1: Add dashboard query and callout to Phase2**

Add `getDashboard` import at the top of `EndDayPage.jsx`:

```javascript
import { getDashboard } from '@/api/dashboard'
```

Add the dashboard query inside the `Phase2` component, after the existing `completedTasks` query:

```javascript
  const { data: dashboardData } = useQuery({
    queryKey: ['dashboard'],
    queryFn: getDashboard,
  })

  const celebrations = dashboardData?.celebrationTasks ?? []
```

Add the celebration callout JSX inside Phase2's return, between the completed tasks `</div>` and the `<form>` tag:

```jsx
      {celebrations.length > 0 && (
        <div className="mb-6">
          <p className="text-xs font-medium text-gray-400 uppercase tracking-wider mb-2">
            Notable today
          </p>
          <div className="space-y-2">
            {celebrations.map((c) => (
              <div
                key={c.taskId}
                className="bg-indigo-50/70 border border-indigo-100 rounded-lg px-4 py-3"
              >
                <p className="text-sm font-medium text-gray-800">{c.taskTitle}</p>
                <p className="text-xs text-gray-500 mt-0.5">
                  {c.projectName} — {c.reason}
                </p>
              </div>
            ))}
          </div>
        </div>
      )}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/pages/EndDayPage.jsx
git commit -m "feat: add celebration callout to evening reflection"
```

---

### Task 5: Update Implementation Plan Checklist

**Goal:** Check off all completed Slice 6 items in the implementation plan spec.

**Files:**
- Modify: `docs/planning/2026-03-30-implementation-plan-design.md`

**Acceptance Criteria:**
- [ ] All completed Slice 6 items are checked off
- [ ] Uncompleted items (UI polish) remain unchecked

**Verify:** Read the spec file and confirm checkboxes match actual work done.

**Steps:**

- [ ] **Step 1: Update checkboxes**

In `docs/planning/2026-03-30-implementation-plan-design.md`, update Slice 6:

```markdown
## Slice 6: Polish & Stats

**Backend:**
- [x] `GET /api/v1/stats/weekly-summary` — tasks completed, total points, total actual_minutes, mood/energy trends for the week
- [x] Celebration logic: identify tasks worth celebrating (high points, high actual_minutes, high effort)

**Frontend:**
- [x] Dashboard weekly summary card: tasks completed, points earned, trends
- [x] Intelligent celebration in Evening Clean-up reflection: highlight significant completions by effort, complexity, time spent
- [ ] UI polish pass: consistent animations/transitions between views, responsive layout adjustments

**Done when:** Dashboard shows weekly stats, evening reflection highlights significant accomplishments, UX feels cohesive.
```

- [ ] **Step 2: Commit**

```bash
git add docs/planning/2026-03-30-implementation-plan-design.md
git commit -m "docs: check off completed Slice 6 items in implementation plan"
```
