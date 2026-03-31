# Planner

ADHD-friendly daily work management tool. Slices 1–3 complete (auth, projects/tasks, deferred items, daily reflection, stats endpoints).

## Quick Start

```bash
# Start everything (DB via Docker, backend via Maven, frontend via Vite)
./start.sh

# Or individually:
docker compose up -d                       # PostgreSQL on :5432
cd backend && mvn spring-boot:run          # Backend on :8080
cd frontend && npm install && npm run dev  # Frontend on :5173
```

- Backend health: http://localhost:8080/actuator/health
- Vite proxies `/api/*` → `http://localhost:8080`

## Testing

```bash
cd backend && mvn test   # Integration tests (require live PostgreSQL)
cd frontend && npm run lint
```

- Backend tests are integration tests that hit a real database — no mocks

## Environment

All have working defaults for local dev:
- `PLANNER_DB_URL` — defaults to `jdbc:postgresql://localhost:5432/planner`
- `PLANNER_DB_USER` / `PLANNER_DB_PASSWORD` — defaults to `planner`
- `JWT_SECRET` — defaults to a dev key (override in production)

## Project Structure

```
backend/src/main/java/com/planner/backend/
  auth/       — JWT login, register, refresh
  task/       — Task CRUD, status, energy level
  project/    — Project CRUD
  deferred/   — Deferred items (inbox)
  reflection/ — Daily reflection
  stats/      — Points/completion stats
frontend/src/
  pages/      — Route-level components
  components/ — Shared UI (QuickCapture, deferred/, AppLayout)
  api/        — TanStack Query + authFetch wrappers
```

## Tech Stack

- **Backend**: Java 21, Spring Boot 3.x, Spring Security (JWT), Spring Data JPA, Flyway
- **Frontend**: React 18, Vite, Tailwind CSS, Radix UI, dnd-kit, TanStack Query
- **Database**: PostgreSQL 16
- **Infra**: Docker Compose

## Key Architecture Decisions

- Flat project list (no area/category entity)
- Task hierarchy via `parent_task_id` (no separate subtask table)
- Child tasks must share parent's `project_id` — enforced in service layer, cascades on parent project change
- Points-based estimation (`points_estimate`), not time-based
- Project status is `is_active` boolean (no priority on projects)
- JWT auth: refresh token in HttpOnly cookie, access token in response body

## Implementation Approach

Vertical slices, built sequentially. See `docs/planning/2026-03-30-implementation-plan-design.md` for the full spec.

The spec is a checklist. Always check off items as they are completed.

## Key Documents

- `docs/ARCHITECTURE.md` — Data model, API endpoints, tech stack
- `docs/IMPLEMENTATION_PLAN.md` — Design decisions, slice overview
- `docs/planning/2026-03-30-implementation-plan-design.md` — Detailed vertical slice spec
- `docs/planning/user_design/` — User design docs (use cases, workflows, wireframes)

## Conventions

- API prefix: `/api/v1/`
- Database migrations: Flyway (SQL files)
- ADHD-friendly UX tone: "Done for now" not "Skip", "Time's up. Good work!" not "Timer expired"

## Design Principles

- Be supportive, not judgemental. Never question the user's decisions. Ask "Is this still important?" not "Why haven't you done this?"
- The goal is to encourage the user to make informed decisions about how to spend their time, not to steer them toward a particular decision.
- Deciding NOT to do something is a victory. Keep task lists small and focused.
- Prefer archive over delete.
- The workflow is a suggestion, not a restriction.