# Planner

ADHD-friendly daily work management tool. Design phase complete, implementation not yet started.

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

## Key Documents

- `docs/ARCHITECTURE.md` — Data model, API endpoints, tech stack
- `docs/IMPLEMENTATION_PLAN.md` — Design decisions, slice overview
- `docs/planning/2026-03-30-implementation-plan-design.md` — Detailed vertical slice spec
- `docs/planning/user_design/` — User design docs (use cases, workflows, wireframes)

## Conventions

- API prefix: `/api/v1/`
- Database migrations: Flyway (SQL files)
- ADHD-friendly UX tone: "Done for now" not "Skip", "Time's up. Good work!" not "Timer expired"
