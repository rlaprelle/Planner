# Planner

ADHD-friendly daily work management tool.

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
cd backend && mvn test          # Integration tests (require live PostgreSQL)
cd frontend && npm run lint     # Lint only — no frontend unit tests yet
cd e2e && npx playwright test   # E2E regression suite (no backend required — all API mocked)
```

- Backend tests are integration tests that hit a real database — no mocks
- E2E tests mock all `/api/*` calls via `page.route()` — just needs Vite running (auto-started by Playwright)

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
  pages/           — Route-level components
  pages/project-detail/ — Task list, detail panel, row components
  layouts/         — App shell (AppLayout)
  components/      — Shared UI (QuickCapture, deferred/)
  api/             — TanStack Query + authFetch wrappers
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

The spec is a checklist. Always check off items as they are completed. **Before pushing or creating a PR, verify all completed work is checked off in the spec** — this is easy to miss at the end of a session.

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

## Frontend Workflow

- When starting a worktree for frontend work, immediately start a dev server from the worktree (e.g., on a different port like 5174) so changes can be previewed throughout
- For mechanical tasks (CSS class replacements, renames): batch and dispatch without per-task reviews. Reserve full reviews for tasks involving judgment.

## Frontend Design Principles

1. **Calm over clever** — The interface should feel quiet and grounding. Avoid visual noise: competing colors, dense layouts, animated distractions. When in doubt, remove rather than add.
2. **Soft shapes, soft colors** — Rounded corners (12-16px for containers, 8-12px for inline elements like badges and inputs). Muted tones from a soft lavender palette — dusty purples, warm grays, gentle off-whites. Avoid harsh borders, high-contrast outlines, and saturated colors except for intentional emphasis.
3. **One thing at a time** — Favor progressive disclosure over showing everything at once. Surface the current step or task prominently; let secondary information recede or be available on demand. Reduce decision fatigue by narrowing what's visible.
4. **Generous breathing room** — Relaxed whitespace and padding throughout. Elements should not feel crowded. Give each item space to be read without competing with its neighbors. Comfortable touch targets, spacious row heights.
5. **Warm, not clinical** — The palette and tone should feel personal and inviting, not sterile or corporate. Slight warmth in backgrounds (tinted off-whites, not pure white), soft shadows over hard borders. The app should feel like opening a journal, not a spreadsheet.
6. **Guide gently** — Use subtle visual hierarchy (tinted backgrounds, font weight, muted color shifts) to guide the eye toward what matters now, without demanding attention. Nothing should shout.