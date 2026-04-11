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
backend/src/main/java/com/echel/planner/backend/
  auth/       — JWT login, register, refresh
  task/       — Task CRUD, deferral, status, energy level
  project/    — Project CRUD
  deferred/   — Deferred items (inbox)
  reflection/ — Daily/weekly/monthly reflection
  schedule/   — Time blocks, schedule management
  stats/      — Points/completion stats
  admin/      — Admin CRUD for all entities, schedule, time blocks
frontend/src/
  pages/                — Route-level components
  pages/project-detail/ — Task list, detail panel/modal, row components
  pages/admin/          — Admin panel (users, projects, tasks, deferred, reflections, time blocks)
  pages/active-session/ — Timer, subtask checklist, chime
  pages/start-day/      — Start day flow components
  auth/                 — AuthContext, ProtectedRoute, useAuth
  contexts/             — ActiveSessionContext
  layouts/              — App shell (AppLayout)
  components/           — Shared UI (QuickCapture, deferred/)
  components/ritual/    — Ritual phase components (TaskTriagePhase, InboxPhase, DailyReflectionPhase, TaskSchedulePhase, CompletionPhase)
  api/                  — TanStack Query + authFetch wrappers (admin, auth, dashboard, deferred, projects, reflection, schedule, tasks)
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
- Task statuses are `OPEN`, `COMPLETED`, `CANCELLED` — enforced by DB CHECK constraint (V10 migration)
- Task deferral via `visible_from` (DATE) and `scheduling_scope` (DAY/WEEK/MONTH) fields on the task table

## Ritual System

The app is organized around 6 rituals: Start Month, Start Week, Start Day, End Day, End Week, End Month.

- **Start rituals** help users schedule active tasks into the future (assign to a week, day, or time block)
- **End rituals** are supersets — End Week includes all End Day phases plus weekly reflection; End Month includes all End Week phases plus monthly reflection
- **End Day phases**: task triage (one-at-a-time review of all active tasks) → inbox processing → daily reflection → completion screen
- **Task triage** is the core mechanic: for each active task, defer (tomorrow/next week/next month), cancel, or keep. This creates pressure to keep the task list small.
- Start Month and Start Week are optional — if skipped, all active tasks simply appear in Start Day

### Active Task Definition

An **active task** is one that is visible and actionable right now. This definition is used consistently across backend queries and frontend filters:

```
status = 'OPEN'
AND archived_at IS NULL
AND parent_task_id IS NULL
AND (visible_from IS NULL OR visible_from <= today)
```

Both backend queries (`findActiveForUser`, `findSuggestedForUser`) and any client-side filtering must apply all four conditions. Missing the `visible_from` check will show deferred tasks that shouldn't be visible yet.

## Worktree Caveats

- All worktrees share a single PostgreSQL container (`planner-db`) and database. Flyway migrations from one branch affect all others.
- If a worktree applies a new migration (e.g. V9 adds columns), then an older branch's backend may fail with Hibernate schema validation errors. The start scripts detect this and suggest `docker compose -p planner down -v` to reset.
- The DB has CHECK constraints enforcing valid status values (`OPEN`, `COMPLETED`, `CANCELLED`). Old code that tries to insert `TODO` or `DONE` will get a constraint violation.

## Key Documents

- `docs/ARCHITECTURE.md` — Data model, API endpoints, tech stack
- `docs/IMPLEMENTATION_PLAN.md` — Design decisions and rationale behind key choices
- `docs/planning/user_design/DEFERRED_WORK.md` — Roadmap of deferred features and future work
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

- When starting a worktree for frontend work, immediately start a dev server from the worktree (e.g., on a different port like 5174) so changes can be previewed throughout. However, if the main worktree already has a dev server running on 5173, use that for visual checks — don't lose track of which port has the current code.
- For mechanical tasks (CSS class replacements, renames): batch and dispatch without per-task reviews. Reserve full reviews for tasks involving judgment.
- After merging dev into a feature branch, do a visual spot-check — merges can introduce UI regressions that aren't caught by tests (e.g., new features from dev appearing in contexts where they don't belong).

## Frontend Design Principles

1. **Calm over clever** — The interface should feel quiet and grounding. Avoid visual noise: competing colors, dense layouts, animated distractions. When in doubt, remove rather than add.
2. **Soft shapes, soft colors** — Rounded corners (12-16px for containers, 8-12px for inline elements like badges and inputs). Muted tones from a soft lavender palette — dusty purples, warm grays, gentle off-whites. Avoid harsh borders, high-contrast outlines, and saturated colors except for intentional emphasis.
3. **One thing at a time** — Favor progressive disclosure over showing everything at once. Surface the current step or task prominently; let secondary information recede or be available on demand. Reduce decision fatigue by narrowing what's visible.
4. **Generous breathing room** — Relaxed whitespace and padding throughout. Elements should not feel crowded. Give each item space to be read without competing with its neighbors. Comfortable touch targets, spacious row heights.
5. **Warm, not clinical** — The palette and tone should feel personal and inviting, not sterile or corporate. Slight warmth in backgrounds (tinted off-whites, not pure white), soft shadows over hard borders. The app should feel like opening a journal, not a spreadsheet.
6. **Guide gently** — Use subtle visual hierarchy (tinted backgrounds, font weight, muted color shifts) to guide the eye toward what matters now, without demanding attention. Nothing should shout.
7. **Evoke physical artifacts** — UI elements should feel like tangible objects: notebooks, index cards, sticky notes. Rounded corners, soft shadows, and subtle depth cues create a tactile quality that makes the digital feel personal and approachable.

## E2E Testing

- `e2e/playwright.config.ts` should not be modified by feature branches — the port (5173) and webServer config are shared defaults. Worktree dev servers should use `BASE_URL` env var if they need a different port, not change the config file.
- When adding new page features, check whether existing E2E tests need a mock for new API endpoints the page now calls (e.g., adding `mockEventsForDate` when the page starts fetching events).

## Known Issues / Quirks

- **Worktrees need `npm install`** — `node_modules` aren't shared across git worktrees. Run `cd frontend && npm install` before starting a dev server in any new worktree.
- **Preview server can't verify auth-gated pages** — The Claude Preview tool has no way to log in, so changes behind authentication can't be spot-checked through it. Use the full dev stack (`./start.sh`) for visual verification of protected routes.
- **ESLint false "unused" warnings** — The linter reports components defined and used within the same file as unused. This is a known quirk of single-file analysis — not real errors. Ignore these warnings.
- **`setState` in `useEffect` lint error** — Several form components (e.g., `ProjectFormModal`) sync local state from props via `useEffect` + `setState`. The linter flags this, but it's the established pattern. When feasible, prefer the `key`-prop remount approach to avoid the warning entirely.