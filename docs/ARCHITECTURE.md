# Planner - Architecture

**The Planner is a daily mindfulness and work management tool designed for ADHD brains.**

For full project vision, design principles, and user flows, see the [user design documents](planning/user_design/).

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 18, Vite, Tailwind CSS, Radix UI, dnd-kit, TanStack Query |
| Backend | Java 21, Spring Boot 3.x, Spring Security (JWT), Spring Data JPA |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Containerization | Docker, Docker Compose |
| CI/CD | GitHub Actions |
| API Doc | OpenAPI/Swagger (springdoc-openapi) |

---

## Data Model (PostgreSQL)

### Core Entities

**app_user**
- id (UUID), email, password_hash, display_name, timezone
- created_at, updated_at

**project** (concrete project, flat list)
- id, user_id, name, description
- color (hex), icon
- is_active (boolean, default true)
- sort_order, archived_at
- created_at, updated_at

**task** (actionable work item, supports hierarchy via parent_task_id)
- id, project_id (required, non-nullable), user_id, title, description
- parent_task_id (nullable; null for top-level tasks, references parent for child tasks)
- **Rule**: Child tasks must have the same project_id as their parent. Enforced in service layer; cascades on parent project change.
- status (TODO/IN_PROGRESS/BLOCKED/DONE/SKIPPED)
- priority (1-5), points_estimate (1-5 complexity), actual_minutes (time spent)
- energy_level (LOW/MEDIUM/HIGH), due_date
- sort_order (for ordering within parent)
- blocked_by_task_id (self-reference: "blocked by another task")
- archived_at, completed_at, created_at, updated_at

**deferred_item** (quick capture)
- id, user_id, raw_text
- is_processed, captured_at, processed_at
- resolved_task_id, resolved_project_id (nullable)
- deferred_until_date (nullable, for re-queueing to future evening ritual)
- deferral_count (how many times deferred)
- created_at, updated_at

**daily_reflection** (end-of-day ritual)
- id, user_id, reflection_date (unique per user per date)
- energy_rating (1-5), mood_rating (1-5)
- reflection_notes (optional)
- is_finalized (whether evening ritual is complete)
- created_at, updated_at

**time_block** (scheduled work)
- id, user_id, block_date
- task_id (nullable for breaks/buffers)
- block_type (WORK/BREAK/BUFFER/ADMIN)
- start_time, end_time (clock times)
- actual_start, actual_end (timestamps)
- was_completed (boolean)
- sort_order (chronological)
- created_at, updated_at

---

## Backend API Endpoints

### Auth
- `POST /api/v1/auth/register` - Create account
- `POST /api/v1/auth/login` - Returns access token + refresh token
- `POST /api/v1/auth/refresh` - Rotate refresh token

### Core CRUD
- Projects, Tasks, Deferred Items (standard REST operations)

### Daily Workflows
- `GET /api/v1/schedule/today` - Today's plan with time blocks
- `POST /api/v1/schedule/today/plan` - Create/replace time blocks atomically
- `GET /api/v1/tasks/suggested?date=&limit=7` - Ranked tasks for planning
- `PATCH /api/v1/time-blocks/{id}/start` - Record actual_start
- `PATCH /api/v1/time-blocks/{id}/complete` - Record actual_end, update task.actual_minutes
- `PATCH /api/v1/time-blocks/{id}/done-for-now` - Mark incomplete (was_completed = false)
- `POST /api/v1/deferred` - Ultra-low-friction capture
- `POST /api/v1/deferred/{id}/convert` - Convert to real task
- `POST /api/v1/deferred/{id}/defer` - Re-queue for future date
- `PATCH /api/v1/deferred/{id}/dismiss` - Mark processed without creating task
- `POST /api/v1/schedule/today/reflect` - Save energy/mood/notes, finalize day

### Stats
- `GET /api/v1/stats/streak` - Consecutive days with both planning (TimeBlocks) and reflection (finalized DailyReflection)
- `GET /api/v1/stats/weekly-summary` - Tasks completed, effort, mood trends

---

## Project Structure

```
backend/src/main/java/com/echel/planner/backend/
  admin/        — Admin CRUD for all entities
  auth/         — JWT login, register, refresh
  common/       — Global exception handling
  deferred/     — Deferred items (inbox)
  event/        — Calendar events
  project/      — Project CRUD
  reflection/   — Daily/weekly/monthly reflection
  schedule/     — Time blocks, schedule management
  stats/        — Points/completion stats
  task/         — Task CRUD, deferral, status, energy level

frontend/src/
  pages/              — Route-level components (react-router-dom)
  auth/               — AuthContext, ProtectedRoute
  contexts/           — ActiveSessionContext
  layouts/            — App shell (AppLayout)
  components/         — Shared UI (QuickCapture, EchelLogo, etc.)
  components/ritual/  — Ritual phase components
  components/ui/      — Reusable primitives (Card, ProgressBar, etc.)
  api/                — TanStack Query + authFetch wrappers

e2e/                  — Playwright E2E tests (all API mocked)
docs/                 — Architecture, design docs, specs
```

---

## Known Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| Timezone handling | Store UTC; use user.timezone for display; convert plan_date boundaries in queries |
| Drag-and-drop optimism | TanStack Query onMutate for optimistic reorder, onError for rollback |
| JWT security | Refresh token in HttpOnly cookie (not localStorage); rotate on each refresh |
| Scope creep | Build phases 1-4 first; validate core loop before extras |

