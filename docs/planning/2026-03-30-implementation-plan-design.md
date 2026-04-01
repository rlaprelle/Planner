# Implementation Plan Design Spec

## Approach

Vertical slices — each slice delivers a complete user-facing feature end-to-end (backend + frontend). Built solo, sequentially. No slice depends on anything not already built in a prior slice.

Tasks should be checked off when they are completed.

---

## Slice 0: Foundation

Stand up the dev environment with auth. No business logic.

**Backend:**
- [x] Spring Boot app with `/health` endpoint
- [x] PostgreSQL in Docker Compose
- [x] Flyway wired up (app_user migration)
- [x] Spring Security with JWT auth: register, login, refresh endpoints
- [x] Refresh token in HttpOnly cookie, access token in response body

**Frontend:**
- [x] React app with Vite (Tailwind CSS, Radix UI configured)
- [x] Login/register pages
- [x] Auth context: store access token, handle refresh, protect routes

**Done when:** Register, log in, and call a protected endpoint from the React app.

---

## Slice 1: Core CRUD (Projects + Tasks)

**Backend:**
- [x] Flyway migrations for `project` and `task` tables
- [x] Project REST: create, list, get, update, archive
- [x] Task REST: create, list (by project), get, update, archive, change status
- [x] `parent_task_id` support: create child tasks, enforce same `project_id` as parent in service layer, cascade project changes to children
- [x] Task ordering: by deadline group (TODAY > THIS WEEK > no deadline), then priority (highest first) within each group

**Frontend:**
- [x] Sidebar navigation: Dashboard, Today, Projects, Inbox
- [x] Projects list view: create, edit, archive, color/icon display
- [x] Task list view within a project: create, edit, archive tasks
- [x] Task Details panel: all fields editable (title, description, project, status, priority, points_estimate, energy_level, due_date), child task management (add, remove, reorder, check off)

**Done when:** Create projects, create tasks within them, create child tasks, browse and edit everything from the UI.

---

## Slice 2: Quick Capture

**Backend:**
- [x] Flyway migration for `deferred_item` table
- [x] `POST /api/v1/deferred` — create deferred item (raw_text only)
- [x] `GET /api/v1/deferred` — list unprocessed items (`is_processed = false` and `deferred_until_date <= today` or null)

**Frontend:**
- [x] Floating capture button visible on all views
- [x] Ctrl+Space global hotkey
- [x] Quick Capture Modal: text input, Enter to save, Escape to cancel
- [x] Visual checkmark + audio chime confirmation on save
- [x] Deferred inbox badge in sidebar showing unprocessed count

**Done when:** Hit Ctrl+Space from any view, type a thought, hit Enter, see confirmation, see inbox count increment.

---

## Slice 3: Evening Clean-up

**Backend:**
- [x] `POST /api/v1/deferred/{id}/convert` — accepts task creation payload (project_id, title, description, due_date, priority, points_estimate, child tasks), creates task, marks item processed, sets `resolved_task_id`
- [x] `POST /api/v1/deferred/{id}/defer` — sets `deferred_until_date`, increments `deferral_count`
- [x] `PATCH /api/v1/deferred/{id}/dismiss` — marks processed
- [x] Flyway migration for `daily_reflection` table
- [x] `POST /api/v1/schedule/today/reflect` — create/update reflection (energy_rating, mood_rating, reflection_notes), set `is_finalized`
- [x] `GET /api/v1/stats/streak` — count consecutive days where TimeBlocks exist AND DailyReflection is finalized

**Frontend:**
- [x] Evening Clean-up view — Phase 1: Deferred items stack (one card at a time, Convert/Defer/Dismiss)
- [x] Convert form: project dropdown, pre-filled title, optional fields (description, due date, priority, points, child tasks toggle)
- [x] Defer options: 1 day / 1 week / 1 month
- [x] "Inbox Zero!" celebration when all items processed
- [x] Evening Clean-up view — Phase 2: Reflection form (energy slider, mood slider, reflection notes, completed tasks display)
- [x] Streak display after saving reflection
- [x] Sidebar link to Evening Clean-up

**Done when:** Process all deferred items one at a time, complete the reflection form, see streak update.

---

## Slice 4: Morning Planning

**Backend:**
- [x] Flyway migration for `time_block` table
- [x] `POST /api/v1/schedule/today/plan` — create/replace time blocks atomically for a date
- [x] `GET /api/v1/schedule/today` — return today's time blocks with associated task data
- [x] `GET /api/v1/tasks/suggested?date=&limit=7` — ranked tasks using ordering logic

**Frontend:**
- [x] Morning Planning view — top panel: task browser with project columns (horizontal scroll), deadline badges (TODAY / THIS WEEK), points display, selection checkboxes
- [x] Morning Planning view — bottom panel: visual calendar timeline (day's hours), "Add to calendar" auto-places selected tasks
- [x] Drag-and-drop (dnd-kit) to rearrange time blocks
- [x] Auto-suggested break blocks between work blocks
- [x] "Confirm plan" button saves time blocks
- [x] Mid-day adjustment: when reopened, completed blocks greyed out, remaining blocks editable
- [x] Dashboard / Home view: today-at-a-glance card, streak tracker, upcoming deadlines, deferred inbox badge, quick action buttons
- [x] Sidebar "Today" link

**Done when:** Browse tasks by project, select them, place on visual timeline, drag to rearrange, confirm plan, reopen mid-day to adjust. Dashboard shows today's overview.

---

## Slice 5: Active Work Session

**Backend:**
- [x] `PATCH /api/v1/time-blocks/{id}/start` — record `actual_start`
- [x] `PATCH /api/v1/time-blocks/{id}/complete` — record `actual_end`, calculate and update `task.actual_minutes`, set `was_completed = true`
- [x] `PATCH /api/v1/time-blocks/{id}/done-for-now` — record `actual_end`, update `task.actual_minutes`, set `was_completed = false`
- [x] Extend: create new adjacent time block for same task

**Frontend:**
- [x] Active Work Session view — full-screen focus mode: task title, project name, countdown timer, progress bar
- [x] Child tasks as interactive checklist (check to mark done, completion animation)
- [x] Action buttons: Complete / Extend / Done for now
- [x] At 100%: gentle chime + "Time's up. Good work!" message
- [x] Extend prompts for duration (15min, 30min, 1hr)
- [x] Timer persists in header bar if user navigates away
- [x] Entry: click "Start" on a time block from Morning Planning or Dashboard

**Done when:** Start a time block, see timer count down, check off child tasks, hear chime at 100%, complete/extend/end the session.

---

## Slice 6: Polish & Stats

**Backend:**
- [ ] `GET /api/v1/stats/weekly-summary` — tasks completed, total points, total actual_minutes, mood/energy trends for the week
- [ ] Celebration logic: identify tasks worth celebrating (high points, high actual_minutes, high effort)

**Frontend:**
- [ ] Dashboard weekly summary card: tasks completed, points earned, trends
- [ ] Intelligent celebration in Evening Clean-up reflection: highlight significant completions by effort, complexity, time spent
- [ ] UI polish pass: consistent animations/transitions between views, responsive layout adjustments

**Done when:** Dashboard shows weekly stats, evening reflection highlights significant accomplishments, UX feels cohesive.
