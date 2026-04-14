# Echel Planner

ADHD-friendly daily work management tool. *Planning that works with your brain, not against it.*

**Brand:** Echel Planner by Echel Software. See `docs/superpowers/specs/2026-04-11-branding-design.md` for logo, colors, and brand guidelines.

## Quick Start

```bash
# Start everything (DB via Docker, backend via Maven, frontend via Vite)
./start.sh

# Or individually:
docker compose up -d                       # PostgreSQL on :5432
cd backend && mvn spring-boot:run          # Backend on :8080
node dev.js start                          # Frontend (port auto-assigned per worktree)
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
  admin/      — Admin CRUD for all entities, schedule, time blocks
  auth/       — JWT login, register, refresh
  common/     — Global exception handling (EntityNotFound, StateConflict, Validation)
  deferred/   — Deferred items (inbox)
  event/      — Calendar events CRUD
  project/    — Project CRUD
  reflection/ — Daily/weekly/monthly reflection
  schedule/   — Time blocks, schedule management
  stats/      — Points/completion stats
  task/       — Task CRUD, deferral, status, energy level
frontend/src/
  pages/                — Route-level components; routes defined in App.jsx (react-router-dom)
  pages/project-detail/ — Task list, detail panel/modal, row components
  pages/admin/          — Admin panel (users, projects, tasks, deferred, reflections, time blocks)
  pages/active-session/ — Timer, subtask checklist, chime
  pages/start-day/      — Start day flow components
  auth/                 — AuthContext, ProtectedRoute, useAuth
  contexts/             — ActiveSessionContext
  layouts/              — App shell (AppLayout)
  components/           — Shared UI (QuickCapture, EchelLogo, FlyAwayCard, deferred/)
  components/ritual/    — Ritual phase components (TaskTriagePhase, InboxPhase, DailyReflectionPhase, TaskSchedulePhase, CompletionPhase)
  components/ui/        — Reusable primitives (Card, CardLabel, ProgressBar)
  api/                  — TanStack Query + authFetch wrappers (client, admin, auth, dashboard, deferred, events, preferences, projects, reflection, schedule, tasks)
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

## Documentation Files

Each documentation file has a defined purpose, audience, and scope. README and CONTRIBUTING are written for humans; this file (CLAUDE.md) is written for coding agents. Overlap between them is intentional — same facts, different format for different readers. When adding content, place it in the file that matches.

| File | Purpose | Audience | Out of scope |
|------|---------|----------|-------------|
| `README.md` | Product description: what the tool is, who it's for, core concepts, design principles, how to run it | Humans discovering the project | Dev workflow, testing details, architecture internals |
| `CONTRIBUTING.md` | Development setup, testing, environment config, worktree caveats | Humans setting up a dev environment | Product description, design rationale, architecture |
| `CLAUDE.md` | Quick start, project structure, conventions, constraints, quirks | Coding agents | Narrative walkthrough, product marketing |
| `docs/ARCHITECTURE.md` | Data model, API endpoints, tech stack, project structure | Both humans and agents | Product-level framing, design principles, dev setup instructions |
| `docs/DESIGN_PRINCIPLES.md` | UX philosophy, visual design language, animation conventions | Both humans and agents doing UX work | Architecture, data model, dev setup |
| `docs/IMPLEMENTATION_PLAN.md` | Next planned phases of development work | Both humans and agents | Completed features, unsequenced backlog ideas |
| `docs/DEFERRED_WORK.md` | Scratch pad for ideas, tech debt, and deferred features captured during development | Both humans and agents | Completed features, sequenced planned work |
| `docs/INTERNATIONALIZATION.md` | i18n approach, namespace assignments, phased implementation plan | Both humans and agents | General architecture, non-i18n conventions |
| `docs/planning/user_design/` | Original design vision — use cases, workflows, wireframes | Historical reference | Do not update to match current implementation — these capture original design intent |
| `docs/research/` | ADHD neuroscience research informing the design | Anyone understanding the evidence base for design decisions | Implementation details, feature specs |

## Conventions

- API prefix: `/api/v1/`
- Database migrations: Flyway (SQL files)
- ADHD-friendly UX tone: "Done for now" not "Skip", "Time's up. Good work!" not "Timer expired"

## Internationalization

All user-facing strings must use `react-i18next`. Import `useTranslation` with the appropriate namespace, add strings to the corresponding JSON file in `frontend/src/locales/en/`, and use `t()` calls — never hardcode English text in JSX. See `docs/INTERNATIONALIZATION.md` for namespace assignments and conventions.

## Design

**Calm, warm, one-thing-at-a-time.** Soft lavender palette, rounded corners, generous whitespace, progressive disclosure. Supportive tone — never judgemental. Read `docs/DESIGN_PRINCIPLES.md` before any UX work (new features, UI changes, microcopy, workflows).

## Frontend Workflow

- **Dev server management:** Use `node dev.js start` **from the project root** (not `frontend/`) to start a frontend dev server. It assigns a deterministic port per worktree (5200–5299 range; 5173 for the main checkout), detects if one is already running, and writes a `.dev-port` file. Use `node dev.js status` to see all running servers, and `node dev.js stop` to shut down. The `.dev-port` file is validated against live state on every operation, so stale files from crashes are cleaned up automatically. Always use this script instead of `npm run dev` directly — it prevents port collisions between worktrees.
- For mechanical tasks (CSS class replacements, renames): batch and dispatch without per-task reviews. Reserve full reviews for tasks involving judgment.
- After merging dev into a feature branch, review the merge:
  - Do a visual spot-check for UI regressions not caught by tests
  - Review auto-resolved files (run `git diff --name-only` on the merge commit) for silent reversions of your branch's work, especially when your branch made broad changes across many files

## E2E Testing

- **Always import from `../fixtures/auth`**, not from `@playwright/test`, for tests on protected pages. The auth fixture pre-mocks `/auth/refresh` and `/deferred` so the app boots into an authenticated state. Without it, pages redirect to login.
- **Use `dev.js` to start a Vite server before running E2E tests**, then pass its port via `BASE_URL`. Do NOT kill other Vite servers — multiple development sessions may be running concurrently. The workflow: `node dev.js start` (from project root) → note the port → `cd e2e && BASE_URL=http://localhost:<port> npx playwright test`. When `BASE_URL` is set, Playwright skips its own `webServer` launch and uses your server directly.
- `e2e/playwright.config.ts` conditionally skips `webServer` when `BASE_URL` is set. The main checkout (no `BASE_URL`) auto-starts Vite on 5173 as the default. Do not modify the config for other reasons in feature branches.
- When adding new page features, check whether existing E2E tests need a mock for new API endpoints the page now calls (e.g., adding `mockEventsForDate` when the page starts fetching events).

## Known Issues / Quirks

- **Worktrees need `npm install`** — `node_modules` aren't shared across git worktrees. Run `cd frontend && npm install` before starting a dev server in any new worktree.
- **Preview server can't verify auth-gated pages** — The Claude Preview tool has no way to log in, so changes behind authentication can't be spot-checked through it. Use the full dev stack (`./start.sh`) for visual verification of protected routes.
- **ESLint false "unused" warnings** — The linter reports components defined and used within the same file as unused. This is a known quirk of single-file analysis — not real errors. Ignore these warnings.
- **`setState` in `useEffect` lint error** — Several form components (e.g., `ProjectFormModal`, `SettingsPage`) sync local state from props via `useEffect` + `setState`. The linter flags this, but it's the established pattern. When feasible, prefer the `key`-prop remount approach to avoid the warning entirely.