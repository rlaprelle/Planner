# Slice 6: Polish & Stats — Design Spec

## Scope

This slice adds weekly stats visibility and intelligent celebration of accomplishments. The UI polish pass (warm palette, animations, transitions) is deferred to a separate design cycle.

### What's included
- Weekly summary endpoint (rolling 7 days)
- Celebration logic for noteworthy completed tasks
- Dashboard weekly summary banner card
- "Notable today" callout in evening reflection

### What's excluded
- Page transitions / animations
- Responsive / mobile layout work
- Visual warmth pass (separate future effort)
- Daily breakdown charts or mini-graphs

---

## Backend: Weekly Summary Endpoint

**Endpoint:** `GET /api/v1/stats/weekly-summary`

**Response:**

```java
record WeeklySummaryResponse(
    int tasksCompleted,
    int totalPoints,
    int totalFocusMinutes,
    int streakDays,
    String energyTrend,
    String moodTrend,
    boolean hasActivity
)
```

### Field definitions

| Field | Source | Logic |
|---|---|---|
| `tasksCompleted` | `Task` | Count where `status = DONE` and `completedAt` within rolling 7-day window |
| `totalPoints` | `Task` | Sum of `points_estimate` for those completed tasks (null treated as 0) |
| `totalFocusMinutes` | `Task` | Sum of `actual_minutes` from tasks with completed time blocks (`wasCompleted = true`) in window |
| `streakDays` | `DailyReflection` | Reuse existing `computeStreak` method |
| `energyTrend` | `DailyReflection` | Compare average `energyRating` of first 3 days vs last 4 days of window |
| `moodTrend` | `DailyReflection` | Same logic as energy, using `moodRating` |
| `hasActivity` | derived | `false` if all counts are zero |

### Trend calculation

- Split 7-day window: days 1-3 (older) vs days 4-7 (recent)
- Compare averages from finalized reflections in each half
- `"improving"` if recent half > older half
- `"declining"` if recent half < older half
- `"steady"` if equal or within 0.5 difference
- `null` if fewer than 2 total finalized reflections in the window

### Window definition

- Rolling 7 days: `today - 6` through `today`, inclusive
- Timezone-aware using `user.getTimezone()`

### New repository queries

- `TaskRepository.countCompletedInDateRange(AppUser user, Instant start, Instant end)` — count of DONE tasks
- `TaskRepository.sumPointsCompletedInDateRange(AppUser user, Instant start, Instant end)` — sum of points_estimate
- `TaskRepository.sumActualMinutesCompletedInDateRange(AppUser user, Instant start, Instant end)` — sum of actual_minutes
- `DailyReflectionRepository.findFinalizedInDateRange(AppUser user, LocalDate start, LocalDate end)` — reflections for trend calc

### Controller

Add `getWeeklySummary()` method to existing `StatsController`, mapped to `GET /api/v1/stats/weekly-summary`.

---

## Backend: Celebration Logic

Celebration data is added to the existing `DashboardResponse` — no new endpoint.

### Updated DashboardResponse

```java
record DashboardResponse(
    int todayBlockCount,
    int todayCompletedCount,
    int streakDays,
    List<DeadlineSummary> upcomingDeadlines,
    int deferredItemCount,
    List<CelebrationTask> celebrationTasks  // NEW
) {
    record CelebrationTask(
        UUID taskId,
        String taskTitle,
        String projectName,
        String reason
    ) {}
}
```

### Qualification rules

A task completed today qualifies if it meets **any** of:

| Criterion | Threshold | Reason text example |
|---|---|---|
| High complexity | `points_estimate >= 5` | "High complexity task" |
| High time invested | `actual_minutes >= 120` | "3 hours of focused work" |
| Long-running | `completedAt - createdAt >= 7 days` | "On your list for 12 days" |

### Behavior

- Only evaluates tasks completed today (reuses existing `findCompletedTodayForUser` query)
- Capped at 3 results, ordered by: highest `actual_minutes` first, then highest `points_estimate`, then oldest `createdAt`
- Each task gets one reason string based on its most impressive qualifying criterion (time > points > age)
- Reason string uses the actual values (e.g., "2.5 hours of focused work", "On your list for 15 days")

---

## Frontend: Dashboard Weekly Summary Banner

### Layout

Full-width card above the existing 2x2 card grid. All 4 existing cards remain unchanged.

```
┌─────────────────────────────────────────────────────────┐
│  Your Week                                              │
│  12 tasks completed · 34 points · 5h 20m focused        │
│  4-day streak · Energy: improving · Mood: steady        │
└─────────────────────────────────────────────────────────┘
┌─────────────────────┐  ┌─────────────────────┐
│  Today at a Glance  │  │  Planning Streak    │
└─────────────────────┘  └─────────────────────┘
┌─────────────────────┐  ┌─────────────────────┐
│  Upcoming Deadlines │  │  Inbox              │
└─────────────────────┘  └─────────────────────┘
```

### Content

- Title: "Your Week"
- Line 1: `{n} tasks completed · {n} points · {h}h {m}m focused`
- Line 2: `{n}-day streak · Energy: {trend} · Mood: {trend}`
- Trends render as text with no icons or color coding (calm design)
- Null trends are omitted (e.g., if no mood data, that segment doesn't appear)

### Empty state

When `hasActivity` is false: "No activity yet this week."

No call-to-action or guilt — just a neutral statement.

### API integration

- New function: `getWeeklySummary()` in `frontend/src/api/dashboard.js`
- Calls `GET /api/v1/stats/weekly-summary`
- Query key: `['stats', 'weekly-summary']`

---

## Frontend: Celebration Callout in Evening Reflection

### Placement

In End Day page Phase 2, between the completed tasks list and the reflection form.

### Visual treatment

- Soft lavender background, slightly warmer tint than surrounding cards
- Small label: "Notable today"
- Each task shows: title, project name, reason text
- Max 3 items
- No dismiss button, no animation — calm and present

### Empty state

If no tasks qualify, the section does not render. No placeholder or "nothing notable" message.

### Data source

`celebrationTasks` from `DashboardResponse`. The End Day page calls `getDashboard()` when Phase 2 begins.

---

## Files to create or modify

### Backend (create)
- `WeeklySummaryResponse.java` — new DTO in `stats/dto/`

### Backend (modify)
- `StatsController.java` — add `getWeeklySummary` endpoint
- `StatsService.java` — add weekly summary computation + celebration logic
- `DashboardResponse.java` — add `CelebrationTask` record and `celebrationTasks` field
- `TaskRepository.java` — add date-range queries
- `DailyReflectionRepository.java` — add date-range query

### Frontend (modify)
- `frontend/src/api/dashboard.js` — add `getWeeklySummary()` function
- `frontend/src/pages/DashboardPage.jsx` — add weekly summary banner card
- `frontend/src/pages/EndDayPage.jsx` — add celebration callout in Phase 2
