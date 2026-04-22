# Testing Strategy

> **Purpose:** Describes what testing is currently in place, per layer, and the known gaps.
> **Audience:** Humans and coding agents.
> **Out of scope:** Near-term planned additions — see [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md).
> **Last reviewed:** 2026-04-20

## Current approach by layer

### Backend (Java / Spring Boot)

- **Integration tests** run via `mvn test` in `backend/`.
- Tests hit a **real PostgreSQL** database via the shared `planner-db` Docker container — no mocks.
- Flyway migrations run against the test DB before tests execute, so schema assertions reflect production shape.
- Coverage includes controllers, services, and repositories; there are no dedicated unit tests for pure logic.
- **Code coverage** is produced by JaCoCo, bound to the `test` phase — HTML report at `backend/target/site/jacoco/index.html` after `mvn test`.
- **Mutation testing** is available via PIT (`mvn pitest:mutationCoverage`) — HTML report at `backend/target/pit-reports/index.html`. No CI gate yet; treat the first report as a baseline.

**Why no mocks:** catches mock/prod divergence bugs at the cost of slower tests. Migrations, CHECK constraints, and cascade behavior are load-bearing for the app's invariants and are exercised directly.

### Frontend (React / Vite)

- **Linting only** via `npm run lint` (ESLint).
- **No unit tests** for components, hooks, or utilities.
- **Storybook** (`npm run storybook`, port 6006) is the component workshop — stories colocated with components (`ComponentName.stories.jsx`), with MSW mocking any API calls. Storybook is used for visual iteration, animation tuning, and dnd interactions where automated assertions can't capture "feels right."

### E2E

- **Playwright** suite in `e2e/`, run via `npx playwright test`.
- All `/api/*` calls are mocked via Playwright's `page.route()` — **no backend required** to run the suite.
- Covers regression of user-facing flows against a real Vite dev server.
- `BASE_URL` env var lets the suite run against any dev server port (via `node dev.js start`).

## Known gaps

- **No CI gate on coverage or mutation score.** JaCoCo and PIT both produce reports, but no threshold enforces a baseline yet; regressions are only caught by manual report review.
- **No frontend unit tests.** Pure-logic utilities, hooks, and component-level behavior are only exercised indirectly via Storybook and E2E. Refactoring a hook relies on visual review + E2E pass rather than fast feedback.
- **No contract testing between frontend and backend.** The Playwright suite mocks `/api/*` with whatever shapes the frontend expects; backend changes that violate those shapes would not be caught by either test suite until deployment. This will become a larger problem if a separate mobile frontend is added.
- **No dedicated security testing.** No SAST on application code, no SCA on dependencies, no dynamic scanning of auth endpoints.
- **No visual regression or accessibility automation.** CSS and layout changes rely on manual spot-checks and Storybook review.

## Commands reference

```bash
cd backend && mvn test                        # Backend integration tests + JaCoCo report
cd backend && mvn pitest:mutationCoverage     # Mutation testing with PIT
cd frontend && npm run lint                   # Lint only
cd frontend && npm run storybook              # Component workshop on :6006
cd e2e && npx playwright test                 # E2E regression suite (backend-free)
```
