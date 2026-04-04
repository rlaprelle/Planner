# Slice 3: Evening Clean-up — Design Spec

## Overview

Slice 3 delivers the "End Day" ritual: processing deferred inbox items one at a time, then completing a daily reflection. It also delivers a functional Inbox page with inline processing actions, and a sidebar reorganization splitting navigation from daily rituals.

---

## Backend

### Deferred Item Actions

Three new endpoints added to `DeferredItemController`:

**`POST /api/v1/deferred/{id}/convert`**
- Request body: `ConvertToTaskRequest` — a new DTO containing `project_id` (UUID, required), `title` (String, required), `description` (optional), `due_date` (LocalDate, optional), `priority` (Short, optional), `points_estimate` (Short, optional)
- Note: `TaskCreateRequest` does not include `project_id` (it's a path param in the normal task creation flow), so this endpoint uses its own DTO and calls `TaskService.create(projectId, request)` internally
- Marks the deferred item `is_processed = true`, sets `processed_at = now()`, sets `resolved_task_id`
- Returns the created `TaskResponse`

**`POST /api/v1/deferred/{id}/defer`**
- Request body: `deferFor` — one of `"1_DAY"`, `"1_WEEK"`, `"1_MONTH"`
- Sets `deferred_until_date` accordingly, increments `deferral_count`
- Returns updated `DeferredItemResponse`

**`PATCH /api/v1/deferred/{id}/dismiss`**
- No request body
- Sets `is_processed = true`, `processed_at = now()`
- Returns updated `DeferredItemResponse`

### Daily Reflection

New package: `com.echel.planner.backend.reflection`

**Migration `V5__create_daily_reflection.sql`**
```sql
CREATE TABLE daily_reflection (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_user(id),
    reflection_date DATE NOT NULL,
    energy_rating SMALLINT NOT NULL CHECK (energy_rating BETWEEN 1 AND 5),
    mood_rating SMALLINT NOT NULL CHECK (mood_rating BETWEEN 1 AND 5),
    reflection_notes TEXT,
    is_finalized BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, reflection_date)
);
```

**`POST /api/v1/schedule/today/reflect`**
- Upserts `DailyReflection` for `(user, today)`
- Request body: `energy_rating`, `mood_rating`, `reflection_notes` (optional), `is_finalized` (boolean)
- Returns `DailyReflectionResponse`

**`GET /api/v1/stats/streak`**
- Counts consecutive days going back from today where `is_finalized = true` exists for the user
- Streak breaks on any day with no finalized reflection
- Note: Slice 4 will extend this to also require TimeBlocks to exist for the day
- Returns `{ "streak": N }`

---

## Frontend

### Sidebar Reorganization (`AppLayout.jsx`)

`NAV_ITEMS` splits into two groups with a visual divider:

**Navigate group**: Dashboard, Projects, Inbox, Today
- Inbox retains the unprocessed-count badge

**Daily rituals group**: Start Day, End Day
- Slightly differentiated visual style (e.g., indigo-tinted background on hover)
- Start Day links to `/start-day` (placeholder, implemented in Slice 4)
- End Day links to `/end-day`

### Inbox Page (`/inbox`)

Replaces the "Coming soon" stub. Renders a list of unprocessed deferred items.

Each item card shows:
- Raw text
- Captured date (relative, e.g. "2 days ago")
- **Convert**, **Defer**, **Dismiss** action buttons

**Convert**: Expands inline (not a modal) revealing a form with:
- Project dropdown (required)
- Title field (pre-filled from raw text)
- Description (optional)
- Due date (optional)
- Priority (optional)
- Points estimate (optional)
- Submit creates the task and removes the card from the list

**Defer**: Shows three inline buttons — `1 day`, `1 week`, `1 month`. Selecting one defers the item and removes the card.

**Dismiss**: First click changes button label to "Sure?". Second click dismisses and removes the card.

Empty state: "Nothing to process."

### Shared Components

`DeferredItemActions` and `ConvertForm` are extracted as shared components used by both the Inbox page and the End Day flow. They accept an `onDone` callback invoked after any action completes, allowing each consumer to handle the result differently (remove from list vs. advance card).

### End Day Flow (`/end-day`)

A single-page two-phase flow.

**Phase 1: Deferred Items**

- Centered card layout showing one item at a time
- Progress indicator: "2 of 5"
- Uses shared `DeferredItemActions` / `ConvertForm`
- Each action calls `onDone`, which advances to the next item
- If no items: skip Phase 1, show brief "Inbox is clear." note, proceed to Phase 2
- After last item: brief "Inbox Zero!" celebration animation before advancing to Phase 2

**Phase 2: Reflection**

Form fields:
- Energy slider (1–5), labelled "Drained" → "Energized"
- Mood slider (1–5), labelled "Rough" → "Great"
- Reflection notes (optional textarea, placeholder: "Anything on your mind?")
- Read-only list of tasks completed today (`status = DONE`, `updated_at` is today) — gives a sense of accomplishment

Submit button label: "Wrap up the day"

**After saving:**
- Displays current streak: "5 days" with a short affirming message (e.g., "You showed up." for streak = 1, "Keep it going." for streak > 1)
- "Done" button returns to Dashboard

---

## Routing

New routes added to `App.jsx`:
- `/inbox` → `InboxPage` (replaces stub)
- `/end-day` → `EndDayPage`
- `/start-day` → `StartDayPage` (stub, "Coming soon")

---

## API Client (`frontend/src/api/`)

New functions in `deferred.js`:
- `convertDeferredItem(id, payload)`
- `deferDeferredItem(id, deferFor)`
- `dismissDeferredItem(id)`

New file `reflection.js`:
- `saveReflection(payload)`
- `getStreak()`

New file `tasks.js` addition:
- `getTodayCompletedTasks()` — calls `GET /api/v1/tasks/completed-today`

### New Backend Endpoint for Completed Tasks

**`GET /api/v1/tasks/completed-today`** added to `TaskController`:
- Returns all non-archived tasks belonging to the user where `status = DONE` and `updated_at >= start of today (UTC)`
- Used by the reflection form to show the accomplishment list
