# E2E Testing Design — Playwright Slice 4 Regression Suite

**Date:** 2026-03-31
**Scope:** Regression tests for Morning Planning (Slice 4) — DashboardPage and StartDayPage
**Approach:** Playwright E2E, frontend-only, all API calls mocked via `page.route()`

---

## Goals

Lock down the Slice 4 behavior (Morning Planning + Dashboard) so future development doesn't break it undetected. Not full-app coverage — focused regression for the flows built in Slice 4.

## Non-Goals

- Testing other pages (login, projects, inbox, end-day) — out of scope for this suite
- Testing backend logic or database queries
- Multi-browser testing (Chromium only)
- Visual regression / screenshot diffing

---

## Architecture

### Test mode: E2E browser tests (not component tests)

Playwright launches a real Chromium browser against the running Vite dev server. It does **not** process source files — Vite handles all transpilation. This avoids the `@dnd-kit/core` ESM-only issue that affects Playwright's component test mode.

### Mock strategy: `page.route()` interception

All `/api/*` requests are intercepted at the browser network layer before they reach the backend. No Java, no PostgreSQL, no Docker required to run the suite. Each test sets up exactly the routes it needs.

### Auth strategy: mock refresh endpoint

`AuthProvider` calls `POST /api/v1/auth/refresh` on mount to restore session. Tests mock this endpoint to return a fake access token before any navigation. The app authenticates automatically without touching the login UI.

---

## Directory Structure

```
e2e/
  package.json          ← standalone package; devDeps: @playwright/test, typescript
  tsconfig.json         ← extends nothing; targets ES2022, moduleResolution node
  playwright.config.ts  ← webServer, Chromium only, baseURL :5173
  fixtures/
    auth.ts             ← `authenticated` base fixture (mocks refresh endpoint)
    mocks.ts            ← per-endpoint route helpers, each accepting optional override
    data.ts             ← typed fixture constants (1 project, 3 tasks, 2 blocks)
  tests/
    dashboard.spec.ts   ← 3 tests: cards, no-plan state, success toast
    start-day.spec.ts   ← 5 tests: task browser, checkbox+add, confirm plan, pre-populate, drag
```

The `e2e/` directory is a sibling of `frontend/` and `backend/`, not nested inside either.

---

## Playwright Config

```ts
// e2e/playwright.config.ts
import { defineConfig } from '@playwright/test'

export default defineConfig({
  testDir: './tests',
  use: {
    baseURL: 'http://localhost:5173',
  },
  projects: [
    { name: 'chromium', use: { browserName: 'chromium' } },
  ],
  webServer: {
    command: 'npm run dev',
    cwd: '../frontend',
    url: 'http://localhost:5173',
    reuseExistingServer: !process.env.CI,
  },
})
```

---

## Fixtures

### `fixtures/data.ts` — shared fixture constants

One realistic dataset shared across all tests.

**Tasks** — shape matches `TaskResponse`:
```ts
{
  id: 'task-1', projectId: 'proj-1', projectName: 'Work', projectColor: '#6366f1',
  title: 'Write tests', status: 'TODO', deadlineGroup: 'NO_DEADLINE',
  pointsEstimate: 2, priority: 3, sortOrder: 0,
  // all other fields null/0/undefined
}
```
Three tasks: "Write tests" (`task-1`), "Review PR" (`task-2`), "Update docs" (`task-3`).

**Time blocks** — shape matches `TimeBlockResponse`:
```ts
{ id: 'block-1', blockDate: '2026-03-31', startTime: '08:00', endTime: '09:00',
  sortOrder: 0, task: { id: 'task-1', title: 'Write tests', status: 'TODO', pointsEstimate: 2 } }
```
Two blocks: "Write tests" 08:00–09:00, "Review PR" 09:00–10:00.

**Dashboard data** — shape matches `DashboardResponse`:
```ts
{ todayBlockCount: 3, todayCompletedCount: 1, streakDays: 5, upcomingDeadlines: [], deferredItemCount: 0 }
```

### `fixtures/auth.ts` — authenticated fixture

```ts
import { test as base } from '@playwright/test'

export const test = base.extend({
  page: async ({ page }, use) => {
    // Auth: mock refresh so AuthProvider auto-authenticates
    await page.route('/api/v1/auth/refresh', route =>
      route.fulfill({
        json: {
          accessToken: 'test-token',
          user: { id: 'u1', displayName: 'Test User', timezone: 'America/New_York' },
        },
      })
    )
    // Sidebar: AppLayout fetches projects on every page — return empty list
    await page.route('/api/v1/projects', route =>
      route.fulfill({ json: [] })
    )
    await use(page)
  },
})
export { expect } from '@playwright/test'
```

All tests import `{ test, expect }` from `fixtures/auth.ts` instead of `@playwright/test`. Auth and sidebar mocks are registered for every test automatically — individual tests only mock the routes specific to their page.

### `fixtures/mocks.ts` — per-endpoint helpers

```ts
export function mockDashboard(page, override = {}) { ... }
export function mockSuggestedTasks(page, tasks = DEFAULT_TASKS) { ... }
export function mockScheduleToday(page, blocks = DEFAULT_BLOCKS) { ... }
export function mockSavePlan(page) {
  // echoes submitted blocks back as response
}
export function mockProjects(page) {
  // returns empty list — satisfies sidebar without cluttering tests
}
```

Each helper wraps `page.route()` and accepts an optional override for the response body, so tests can vary one field (e.g. `todayBlockCount: 0`) without duplicating fixture data.

---

## Test Scenarios

### `tests/dashboard.spec.ts`

**Test 1: Cards show mocked data**
- Setup: `mockDashboard` with default data (`todayBlockCount: 3`, `todayCompletedCount: 1`, `streakDays: 5`)
- Navigate to `/`
- Assert: text "1 / 3" visible, progress bar `div` has non-zero `width` style, text "5" visible in streak card

**Test 2: No-plan state**
- Setup: `mockDashboard` with `todayBlockCount: 0`
- Navigate to `/`
- Assert: "No plan yet." visible, "Start planning →" link present

**Test 3: Success toast appears and disappears**
- Setup: `mockDashboard` with default data
- Navigate to `/`, then inject state via `page.evaluate(() => history.replaceState({ successMessage: 'Plan saved. Good luck today!' }, ''))`, then `page.reload()`
- Assert: toast text "Plan saved. Good luck today!" visible
- Wait for toast to become hidden: `page.waitForSelector('.toast-text', { state: 'hidden' })` (timeout: 5s)
- Assert: toast no longer in DOM

### `tests/start-day.spec.ts`

**Test 1: Task browser renders project columns**
- Setup: `mockSuggestedTasks` with 2 tasks in "Work" project, `mockScheduleToday` with empty blocks
- Navigate to `/start-day`
- Assert: "Work" column header visible, both task titles visible

**Test 2: Checkbox add to calendar**
- Setup: suggested tasks, empty schedule
- Navigate to `/start-day`
- Check the checkbox on "Write tests"
- Click "+ Add to calendar"
- Assert: "Write tests" appears in the time grid section

**Test 3: Confirm plan navigates with toast**
- Setup: suggested tasks, `mockScheduleToday` returns 1 existing block, `mockSavePlan`
- Navigate to `/start-day`
- Click "Confirm plan"
- Assert: navigated to `/`, toast "Plan saved. Good luck today!" visible

**Test 4: Mid-day replanning pre-populates calendar**
- Setup: `mockScheduleToday` returns 2 blocks ("Write tests" 08:00–09:00, "Review PR" 09:00–10:00)
- Navigate to `/start-day`
- Assert: both task titles visible in the time grid

**Test 5: Drag task card onto calendar**
- Setup: suggested tasks with "Write tests", empty schedule
- Navigate to `/start-day`
- Locate "Write tests" task card
- Drag it to the centre of the time grid element
- Assert: "Write tests" appears in the time grid

---

## Toast Navigation Approach

Two tests exercise the success toast:

- **Dashboard test 3** (isolated toast test): uses `history.replaceState` to inject `successMessage` into location state, then reloads. Tests the toast component in isolation.
- **Start-day test 3** (confirm plan): the toast appears naturally after `navigate('/', { state: ... })` in `saveMutation.onSuccess`. Tests the full confirm-plan flow end-to-end.

Both approaches are correct for their respective purposes. Do not use `history.replaceState` in start-day test 3 — let the real navigation path produce the toast.

---

## Running the Suite

```bash
# From e2e/
npm install
npx playwright test

# With UI (recommended locally)
npx playwright test --ui

# Single file
npx playwright test tests/dashboard.spec.ts
```

Vite starts automatically if not already running. On CI, set `CI=true` to prevent reuse of an existing server.

---

## Known Constraints

- **Drag test reliability:** `dragTo` on the time grid is inherently less stable than click-based tests. The drag test asserts only that a block appears — not its exact position. If it proves flaky, replace with the checkbox+add flow as an equivalent coverage path.
- **Toast timing:** The auto-dismiss test waits 4 seconds. Use `page.waitForSelector(..., { state: 'hidden' })` rather than a fixed `sleep` to avoid flakiness.
- **`@dnd-kit/core` ESM:** Not an issue in this setup. Playwright never processes source files; Vite handles all transforms.
- **Sidebar routes:** The `AppLayout` sidebar may fire additional API requests (projects list, etc.). `mockProjects` returns an empty list to satisfy these without affecting test assertions.
