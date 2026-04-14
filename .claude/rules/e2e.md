---
paths:
  - "e2e/**"
---

# Planner E2E conventions

## Auth fixture

Import from `../fixtures/auth` for tests on authenticated pages — the fixture pre-mocks `/auth/refresh`, `/deferred`, and `/user/preferences` so the app boots into an authenticated state. Without it, protected pages redirect to login.

Import from `@playwright/test` directly only when the test exercises the auth flow itself (see `auth.spec.ts`, `admin.spec.ts`). Using the auth fixture there would short-circuit the behavior under test.

## Mock helpers

Add new API mocks as reusable helpers in `fixtures/mocks.ts` (e.g., `mockDashboard`, `mockSuggestedTasks`, `mockEventsForDate`). Do not inline `page.route()` calls in specs.

When a page starts calling a new endpoint, audit existing specs for that page and add the mock — a missing mock silently 404s and flakes the test.
