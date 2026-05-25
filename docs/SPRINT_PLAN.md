# Sprint Plan

> **Purpose:** Next planned phases of development work.
> **Audience:** Engineers, coding agents, and anyone planning near-term work.
> **Out of scope:** Completed features, unsequenced ideas — see [GitHub Issues](https://github.com/rlaprelle/Planner/issues).
> **Last reviewed:** 2026-04-20

No phases queued. Two prior phases — verifiable test thoroughness (PIT + JaCoCo) and admin panel authentication & authorization — have landed; the shipping artefacts live in `backend/pom.xml`, `backend/src/main/java/com/echel/planner/backend/auth/`, and the admin-authorization tests under `backend/src/test/java/com/echel/planner/backend/admin/`.

Candidates for a future phase (pull from [GitHub Issues](https://github.com/rlaprelle/Planner/issues) when sequenced):

- Frontend coverage (Vitest `v8`) and frontend mutation testing (Stryker).
- Security scanning (Semgrep, OWASP Dependency-Check).
- Contract testing (Pact).
- Visual regression.
- Admin promotion flow (turning a regular user into an admin without manual SQL).
- Enforce a mutation-score threshold in CI once we've lived with a PIT baseline for a few weeks.
