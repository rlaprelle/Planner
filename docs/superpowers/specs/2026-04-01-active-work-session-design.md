# Active Work Session — Design Spec (Slice 5)

## Overview

Full-screen focus mode for working through scheduled time blocks. The user starts a block from Morning Planning or the Dashboard, sees a centered countdown timer with optional subtask checklist, and ends the session by completing, extending, or stepping away.

---

## Backend

### Migration (V7)

Add three columns to `time_block`:

| Column | Type | Default | Nullable |
|--------|------|---------|----------|
| `actual_start` | TIMESTAMP WITH TIME ZONE | — | yes |
| `actual_end` | TIMESTAMP WITH TIME ZONE | — | yes |
| `was_completed` | BOOLEAN | false | no |

### Endpoints

**`PATCH /api/v1/time-blocks/{id}/start`**
- Sets `actual_start = now()`
- Rejects (409) if `actual_start` is already set
- Returns updated TimeBlockResponse

**`PATCH /api/v1/time-blocks/{id}/complete`**
- Sets `actual_end = now()`, `was_completed = true`
- Calculates elapsed minutes: `actual_end - actual_start`
- Adds elapsed minutes to `task.actual_minutes` (accumulates, does not replace)
- Sets `task.status = DONE`
- Returns updated TimeBlockResponse

**`PATCH /api/v1/time-blocks/{id}/done-for-now`**
- Sets `actual_end = now()`, `was_completed = false`
- Calculates elapsed minutes and adds to `task.actual_minutes`
- Task status remains unchanged (stays IN_PROGRESS or whatever it was)
- Returns updated TimeBlockResponse

**`POST /api/v1/time-blocks/{id}/extend`**
- Request body: `{ "durationMinutes": 15 | 30 | 60 }`
- Creates a new time block for the same task and date
- New block starts at the current block's `end_time`, ends `durationMinutes` later
- Sort order placed immediately after the current block
- Returns the new TimeBlockResponse (frontend navigates to it)

### actual_minutes accumulation

Each complete or done-for-now call adds the session's elapsed minutes to `task.actual_minutes`. Multiple sessions on the same task accumulate. Example: two 30-minute sessions → `actual_minutes = 60`.

---

## Frontend

### Route

`/session/:blockId` — Active Work Session view.

### Entry Points

1. **Morning Planning (StartDayPage):** Add a "Start" button to each time block. Clicking navigates to `/session/:blockId`.
2. **Dashboard (DashboardPage):** Add a "Start" button on the next upcoming block in the today-at-a-glance card. Navigates to `/session/:blockId`.

### Focus View — Centered Minimal Layout

Full-screen, no sidebar. Soft lavender background (`#f8f7fc` or similar from existing palette).

Vertical stack, centered:
1. **Project name** — small, uppercase, muted, letter-spaced
2. **Task title** — larger, semibold
3. **Circular countdown timer** — hero element
   - SVG circle with animated arc showing progress
   - Large time remaining in center (e.g., "23:41")
   - "of 45 min" label below
4. **Subtasks checklist** — only if the task has child tasks. If there are no child tasks, this section is completely omitted from the DOM (no empty frame, no placeholder).
   - Each subtask has a checkbox
   - Checking calls `PATCH /api/v1/tasks/{childId}` to set status = DONE
   - Completed items get strikethrough + muted color
   - Optimistic update via TanStack Query
5. **Action buttons** — horizontal row
   - **Complete** (primary purple) — ends session successfully
   - **Extend** (secondary) — opens duration popover
   - **Done for now** (secondary) — ends session, task stays in-progress

### Timer Behavior

- Calculates remaining time: block `end_time` (as clock time today) minus current time
- Counts down every second via `setInterval`
- At 0:00: chime plays, timer text changes to "Time's up. Good work!", ring fills completely, soft pulse animation
- Timer can go negative: shows "+0:32" in muted text. No hard stop, no modal, no urgency.

### Chime

Synthesized via Web Audio API, same pattern as QuickCapture's `playChime()` but with a warmer tone (lower fundamental frequency, longer sustain). Wrapped in try-catch for silent fallback if AudioContext is unavailable.

### Action Flows

**Complete:**
1. Call `PATCH /time-blocks/{id}/complete`
2. Brief "Nice work!" flash (1.5s, fades out)
3. Navigate back via `navigate(-1)` (browser history), falling back to `/` (Dashboard)

**Done for now:**
1. Call `PATCH /time-blocks/{id}/done-for-now`
2. Navigate back via `navigate(-1)`, falling back to `/` (Dashboard). No celebration.

**Extend → select duration (15min / 30min / 1hr):**
1. Call `POST /time-blocks/{id}/extend` with chosen duration
2. Navigate to `/session/:newBlockId` with the returned block
3. Timer resets seamlessly with the new duration

### Header Timer Persistence

When a session is active and the user navigates away, a compact timer widget appears in the AppLayout header:

- Shows: truncated task title + countdown (e.g., "Design wireframes — 23:41")
- Clicking navigates back to `/session/:blockId`
- Disappears when session ends

**Implementation:** `ActiveSessionContext` (React context) holds:
- `activeBlockId`
- `taskName`
- `endTime` (Date object)

The session page sets context on mount, clears on complete/done-for-now. The header reads from context. Timer in header calculates remaining from `endTime - Date.now()`, same as focus view.

On page refresh, context is lost. Header timer won't show until the user revisits `/session/:blockId`, which re-fetches block data and restores context. Acceptable simplicity tradeoff.

### API Layer

New functions in `frontend/src/api/schedule.js`:
- `startTimeBlock(blockId)` — PATCH start
- `completeTimeBlock(blockId)` — PATCH complete
- `doneForNowTimeBlock(blockId)` — PATCH done-for-now
- `extendTimeBlock(blockId, durationMinutes)` — POST extend

Each wrapped with `authFetch` and exposed as TanStack Query mutations.

---

## Design Principles Applied

- **Calm over clever:** Centered minimal layout, no visual noise, gentle completion message
- **One thing at a time:** Full-screen focus, subtasks only shown if they exist
- **Generous breathing room:** Single centered column, lots of whitespace
- **Warm, not clinical:** Soft lavender palette, rounded elements, encouraging language
- **Guide gently:** Timer notification is informational ("Time's up. Good work!"), not demanding

---

## Out of Scope

- Break block sessions (break blocks have no task — skip for now)
- Notifications/sounds during the session (only the completion chime)
- Keyboard shortcuts within the session view
- Task description display in focus view (title + subtasks is sufficient)
