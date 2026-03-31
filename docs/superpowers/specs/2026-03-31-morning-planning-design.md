# Slice 4: Morning Planning — Design Spec

**Date:** 2026-03-31
**Status:** Approved
**Slice:** 4 of the vertical slice implementation plan

---

## Overview

Slice 4 delivers two major features: the Morning Planning view (`/start-day`) and the Dashboard (`/`). Together they form the daily entry point — users arrive at the dashboard for an overview of their day, then move to Morning Planning to build their schedule. Slices 0–3 (auth, projects/tasks, deferred items, reflection) are prerequisites and are complete.

---

## Data Model

### New table: `time_block`

```sql
id            UUID PRIMARY KEY DEFAULT gen_random_uuid()
user_id       UUID NOT NULL REFERENCES app_user(id)
block_date    DATE NOT NULL
task_id       UUID REFERENCES task(id)   -- nullable, reserved for future non-task blocks
start_time    TIME NOT NULL              -- 15-min granularity enforced in service layer
end_time      TIME NOT NULL              -- minimum 15 min after start_time
sort_order    INTEGER NOT NULL DEFAULT 0
```

**Constraints and rules:**
- `start_time` and `end_time` must be multiples of 15 minutes — enforced in the service layer, not the DB
- `end_time > start_time` — enforced in the service layer
- No DB-level overlap constraint — enforced by the service layer on save and by the push algorithm on the frontend
- Work day bounds: 8 AM–5 PM (hardcoded in MVP; configurable work hours are deferred — see `DEFERRED_WORK.md`)
- `block_type` and `was_completed` fields from the original spec are deferred until Slice 5 (active work sessions)
- No `created_at`/`updated_at` — time blocks are ephemeral planning artifacts replaced atomically each day

---

## Backend API

### `GET /api/v1/schedule/today`

Returns today's time blocks with embedded task details.

**Response:**
```json
[
  {
    "id": "uuid",
    "blockDate": "2026-03-31",
    "startTime": "09:00",
    "endTime": "10:00",
    "sortOrder": 0,
    "task": {
      "id": "uuid",
      "title": "Fix login bug",
      "projectId": "uuid",
      "projectName": "Auth",
      "projectColor": "#6366f1",
      "status": "TODO",
      "pointsEstimate": 3
    }
  }
]
```

- Filters by authenticated user and `block_date = today` (user's timezone)
- Ordered by `sort_order ASC`
- Used by both the Morning Planning calendar and the Dashboard "today at a glance" card

### `POST /api/v1/schedule/today/plan`

Atomically replaces all time blocks for a given date.

**Request:**
```json
{
  "blockDate": "2026-03-31",
  "blocks": [
    { "taskId": "uuid", "startTime": "09:00", "endTime": "10:00" },
    { "taskId": "uuid", "startTime": "10:00", "endTime": "11:30" }
  ]
}
```

**Behavior:**
- Runs in a single transaction: deletes all existing blocks for `blockDate`, inserts the new set
- Validates: 15-min granularity, `end > start`, no overlaps, all times within 8 AM–5 PM
- `sort_order` is assigned by position in the array (0, 1, 2, …)
- Returns the created blocks (same shape as `GET /api/v1/schedule/today`)
- Returns `422 Unprocessable Entity` with detail message on validation failure

### `GET /api/v1/tasks/suggested?date=&limit=`

Returns ranked TODO tasks for the Morning Planning task browser.

**Query params:**
- `date` — defaults to today (user's timezone)
- `limit` — defaults to 50; no practical upper bound in MVP

**Ordering:**
1. Deadline group: TODAY (due_date ≤ today) → THIS_WEEK (due_date ≤ today+7) → NO_DEADLINE
2. Priority descending (1 = highest)

**Filters:**
- `status IN (TODO, IN_PROGRESS)` — excludes DONE, BLOCKED, SKIPPED. IN_PROGRESS tasks are included so the user can reschedule work already started.
- `archived_at IS NULL`
- Excludes tasks already scheduled on `date` (i.e., tasks with a `time_block` for that date)

**Response:** array of `TaskResponse` (existing DTO), grouped data is assembled client-side

### `GET /api/v1/stats/dashboard`

Returns all data needed by the dashboard in a single call.

**Response:**
```json
{
  "todayBlockCount": 4,
  "todayCompletedCount": 1,
  "streakDays": 7,
  "upcomingDeadlines": [
    {
      "taskId": "uuid",
      "taskTitle": "Fix login bug",
      "projectName": "Auth",
      "projectColor": "#6366f1",
      "dueDate": "2026-03-31",
      "deadlineGroup": "TODAY"
    }
  ],
  "deferredItemCount": 3
}
```

- `todayCompletedCount`: time blocks for today where associated task `status = DONE`
- `streakDays`: consecutive days ending today (inclusive) where `daily_reflection.is_finalized = true`. If today has no finalized reflection, returns streak through yesterday.
- `upcomingDeadlines`: up to 5 tasks with `due_date IS NOT NULL`, ordered by `due_date ASC`, `status != DONE`, `archived_at IS NULL`
- `deferredItemCount`: unprocessed deferred items visible today (same logic as `GET /api/v1/deferred`)

---

## Frontend

### Route: `/start-day` → `StartDayPage`

The Morning Planning view. Three stacked rows.

**State managed by `StartDayPage`:**
- `selectedTaskIds: Set<string>` — tasks checked in any row; shared across all three rows
- `blocks: TimeBlockData[]` — current plan in the calendar (local state, not yet saved)
- `isSaving: boolean`

**Data fetching:**
- `useSuggestedTasks(date)` → `GET /api/v1/tasks/suggested`
- `useScheduleToday()` → `GET /api/v1/schedule/today` — pre-populates calendar on load (mid-day replanning)
- `useSavePlan()` → `POST /api/v1/schedule/today/plan`

#### Row 1 — All Tasks Browser

- Horizontally scrollable project columns
- Each column: project name header + list of task cards with checkboxes
- Task card shows: title, deadline badge (TODAY/THIS WEEK in red/amber), points estimate
- Selecting a task already on the calendar disables its checkbox
- Footer: "Selected: N tasks" · "+ Add to calendar" button

#### Row 2 — Deadline Rows (same row, side by side)

- **Left half — Due Today** (red border, soft red background): client-side filter of suggested tasks where `deadlineGroup = 'TODAY'`; same project-column layout; same shared selection state
- **Right half — Due This Week** (amber border, soft amber background): client-side filter where `deadlineGroup = 'THIS_WEEK'`
- Projects with no tasks matching the filter show a "Nothing due" placeholder
- Selecting a task in Row 2 also checks it in Row 1 (shared `selectedTaskIds`)

#### Row 3 — TimeBlockGrid

See component design below.

**"Add to calendar" behavior:**
- Appends selected tasks as 1-hour blocks starting after the last scheduled block (or `dayStart` — 8 AM — if calendar is empty)
- If adding blocks would push past 5 PM, blocks are added up to the limit; a soft warning is shown: "Some tasks didn't fit — you can resize blocks to make room"

**"Confirm plan" button:**
- Calls `POST /api/v1/schedule/today/plan`
- Disabled if no blocks are scheduled
- On success: navigate to `/` with a brief toast: "Plan saved. Good luck today!"

**Mid-day replanning:**
- If `/start-day` is opened when blocks already exist for today, the existing plan pre-populates the calendar
- Blocks whose associated task `status = DONE` are rendered greyed out and non-interactive (not draggable, not resizable)
- Confirming sends all blocks (including completed ones) to preserve the full day's record

---

### Component: `TimeBlockGrid`

The horizontal planner calendar. The most complex component in Slice 4.

**Props:** `blocks`, `onBlocksChange`, `dayStart = "08:00"`, `dayEnd = "17:00"`

**Layout:**
- Fixed-height grid with vertical lines at each hour and lighter lines at each 15-min mark
- Time labels along the top (8 AM, 9 AM, … 5 PM)
- Blocks positioned absolutely: `left = (startMinutes - dayStartMinutes) / totalMinutes × 100%`, `width = durationMinutes / totalMinutes × 100%`

**Subcomponents:**
- **`TimeBlock`** — renders a single block. Shows task title and time range. Has a drag handle (full block body, via dnd-kit `useDraggable`) and a resize handle (right edge, raw mouse events). Greyed-out variant for completed blocks.
- **`useTimeGrid(blocks, dayStart, dayEnd)`** — custom hook encapsulating:
  - Pixel ↔ time conversion (`pixelsToMinutes`, `minutesToPercent`)
  - Snap-to-15 helper
  - `pushBlocks(blocks, changedIndex)` — the push algorithm (see below)
  - Drag state and resize state
  - All handlers passed to `TimeBlockGrid` and `TimeBlock`

#### Drag-to-move (within calendar)

- dnd-kit `DndContext` wraps both the task browser and the calendar
- `TimeBlock` uses `useDraggable`; `TimeBlockGrid` uses `useDroppable`
- During drag: block follows pointer horizontally, snapping to 15-min grid
- On `DragEnd` (source = calendar): compute new start time from pixel offset, run `pushBlocks`, call `onBlocksChange`
- Cannot move before `dayStart` or after `dayEnd`; drop is rejected (block snaps back) if push chain would exceed `dayEnd`

#### Cross-panel drag (task browser → calendar)

- Task cards in the browser rows also use dnd-kit `useDraggable`
- On `DragEnd` (source = task browser, target = calendar): compute start time from pointer position over grid, create a new 1-hour block at that position, run `pushBlocks`
- If the task is already on the calendar (disabled checkbox state), dragging is also disabled

#### Drag-to-resize

- Resize handle: a narrow div on the right edge of each `TimeBlock`
- Implemented with raw `onMouseDown` / `mousemove` / `mouseup` (not dnd-kit — dnd-kit doesn't model resize natively)
- On `mousedown` on handle: capture pointer, record initial `clientX` and block `endTime`
- On `mousemove`: compute `Δx` → `Δminutes` → snap to 15 → new `endTime`; minimum block size 15 min
- Run `pushBlocks` on every `mousemove` tick (real-time visual feedback)
- On `mouseup`: finalize, call `onBlocksChange`
- Cap: if resize would push chain past `dayEnd`, cap `endTime` at the point where the last block fits

#### Push algorithm (`pushBlocks`)

```
function pushBlocks(blocks, changedIndex):
  sort blocks by startTime
  for i from changedIndex+1 to blocks.length-1:
    if blocks[i].startTime < blocks[i-1].endTime:
      shift = blocks[i-1].endTime - blocks[i].startTime
      blocks[i].startTime += shift
      blocks[i].endTime += shift
    else:
      break  // gap exists, no further pushing needed
  return blocks
```

- Pure function (no side effects); called on every drag/resize event for real-time feedback
- Cap: if `blocks[last].endTime > dayEnd`, the initiating drag/resize is rejected

---

### Route: `/` → `DashboardPage`

2×2 card grid. Data from `useDashboard()` → `GET /api/v1/stats/dashboard`.

**Card 1 — Today at a Glance**
- Scheduled block count and completed count: "2 of 4 tasks done"
- Progress bar: `completedCount / blockCount`
- If no blocks scheduled: "No plan yet" + "Start planning →" link to `/start-day`

**Card 2 — Streak Tracker**
- "7-day planning streak" with a simple flame or star indicator
- If streak = 0: "Start your streak tonight — finish today's reflection"
- Note: streak redesign (ratio framing, shame prevention) is deferred — see `DEFERRED_WORK.md`

**Card 3 — Upcoming Deadlines**
- Up to 5 tasks, ordered by due date
- Each row: project color swatch · task title · due date badge (TODAY in red, THIS WEEK in amber)
- Clicking a row navigates to `/start-day`
- If none: "No upcoming deadlines. Nice."

**Card 4 — Inbox**
- "N items waiting" where N = unprocessed deferred items
- If 0: "Inbox clear"
- Click navigates to `/inbox`

**Quick actions (below cards):**
- "Start Morning Planning" → `/start-day`
- "Evening Clean-up" → `/end-day`

---

## New API Hooks

| Hook | Endpoint | Notes |
|------|----------|-------|
| `useScheduleToday()` | `GET /api/v1/schedule/today` | Used by the Morning Planning calendar; dashboard uses `useDashboard()` |
| `useSuggestedTasks(date)` | `GET /api/v1/tasks/suggested` | Pre-filtered client-side for deadline rows |
| `useSavePlan()` | `POST /api/v1/schedule/today/plan` | Mutation; invalidates `schedule` and `dashboard` queries |
| `useDashboard()` | `GET /api/v1/stats/dashboard` | Single call for all dashboard data |

---

## Deferred from this Slice

- `block_type` field (WORK / BREAK / BUFFER / ADMIN) — Slice 5
- `was_completed` field — Slice 5
- `actual_start` / `actual_end` tracking — Slice 5
- Configurable work day hours (currently hardcoded 8 AM–5 PM) — see `DEFERRED_WORK.md`
- Break block auto-insertion — see `DEFERRED_WORK.md`
- Streak redesign (ratio framing / shame prevention) — see `DEFERRED_WORK.md`
