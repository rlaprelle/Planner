# Changelog

All notable changes to Echel Planner are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-05-25

Initial public pre-release of Echel Planner.

### Added

- **Ritual system** — Start Day, End Day, Start Week, End Week, Start Month, End Month guided workflows. End rituals chain task triage, inbox processing, and reflection phases; End Week and End Month layer weekly/monthly reflection on top of End Day.
- **Task triage** — one-at-a-time review of every active task with keep / defer (tomorrow, next week, next month) / cancel decisions. Deferral implemented via `visible_from` and `scheduling_scope` on the task.
- **Projects and tasks** — flat project list, task hierarchy via `parent_task_id`, points-based estimation, `OPEN` / `COMPLETED` / `CANCELLED` status (DB-enforced).
- **Quick Capture** — global capture shortcut for stray thoughts; entries land in the deferred inbox for processing during End Day.
- **Active sessions** — focused work mode with countdown timer, subtask checklist, and a soft chime; complete / extend / done-for-now controls at expiry.
- **Daily schedule and time blocks** — drag-and-drop scheduling of tasks onto a visual day with configurable time blocks.
- **Authentication** — JWT login and registration, HttpOnly refresh-token cookie, access token in response body.
- **Admin panel** — CRUD over users, projects, tasks, deferred items, reflections, and time blocks. Gated by `ROLE_ADMIN` in Spring Security with defense-in-depth `AdminRoute` on the frontend.
- **User preferences** — default start/end time, session minutes, week start day, ceremony day, and locale on the user record; exposed via `/api/preferences` and the Settings page.
- **Internationalization scaffolding** — i18next + react-i18next with per-namespace JSON catalogs and a phased rollout plan.
- **Storybook component workshop** — Storybook 10 with MSW for mocked API responses; used for visual polish, animation tuning, and dnd interaction iteration.
- **Test suites** — backend integration tests against a live PostgreSQL, frontend Vitest unit tests, and a Playwright E2E regression suite with all `/api/*` calls mocked at the network layer.
- **Per-worktree dev server orchestration** — `dev.js` assigns deterministic Vite ports per worktree to prevent collisions between concurrent development sessions.
- **GPL v3 licensing** — declared in `LICENSE`, `README.md`, `pom.xml`, and `package.json`.

[Unreleased]: https://github.com/rlaprelle/Planner/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/rlaprelle/Planner/releases/tag/v0.1.0
