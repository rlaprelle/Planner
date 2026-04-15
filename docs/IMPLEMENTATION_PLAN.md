# Implementation Plan

> **Purpose:** Next planned phases of development work.
> **Audience:** Engineers, coding agents, and anyone planning near-term work.
> **Out of scope:** Completed features, unsequenced ideas — see [DEFERRED_WORK.md](DEFERRED_WORK.md).
> **Last reviewed:** 2026-04-14

## Phase: Verifiable test thoroughness (backend)

**Motivation:** Current backend integration tests hit a real Postgres and exercise controllers/services/repositories, but there is no measure of whether the tests are thorough. We can't answer "is this code tested?" or "would my tests catch a bug if the logic were wrong?" See [TESTING_STRATEGY.md § Known gaps](TESTING_STRATEGY.md#known-gaps).

Both additions target the Java/Maven backend. Each is a standalone adoption — they can be tackled in either order or in parallel.

### Option A — Mutation testing with PIT ⭐ higher signal

PIT ([pitest.org](https://pitest.org/)) is a Maven plugin that seeds small mutations into compiled bytecode (flipping `>` to `<`, removing a `return`, replacing `true` with `false`, etc.), re-runs the existing JUnit test suite, and reports every mutation that **survived** — i.e., tests that didn't notice the code was broken. Survivors mark real test-coverage gaps; killed mutations are evidence the tests actually guard that logic.

**Tasks:**

- Add the `pitest-maven` plugin to `backend/pom.xml` with the `pitest-junit5-plugin` for JUnit 5 support.
- Configure initial scope: mutate `com.echel.planner.backend.*`, skip DTOs and generated code.
- Run `mvn pitest:mutationCoverage` locally; review the HTML report at `target/pit-reports/`.
- Identify the 3–5 highest-value classes with surviving mutations (ritual state logic, active-task query, deferral rules, CHECK-constraint-adjacent code) and add tests to kill them.
- Add a CI job that runs PIT against changed code only (`pitest.scmCommit` / `features=+GIT(from=HEAD~1)`) to keep CI fast.
- Decide on a mutation score threshold to track over time; do **not** fail the build on it until we've lived with a baseline for a few weeks.

**Caveats:**

- Full mutation runs are slow. Gate full runs to nightly or main-branch merges; incremental (changed-code-only) runs are fast enough for per-PR CI.
- PIT operates on bytecode, so it automatically targets whatever JUnit discovers — no test changes required to adopt it.

### Option B — Code coverage with JaCoCo

JaCoCo is the standard Maven plugin for Java coverage. It answers the weaker-but-complementary question: "what code did tests even touch?" PIT only mutates code that was executed by a test, so JaCoCo's gaps are also PIT's blind spots.

**Tasks:**

- Add the `jacoco-maven-plugin` to `backend/pom.xml` with `prepare-agent` and `report` goals bound to `test` and `verify`.
- Generate the HTML report at `target/site/jacoco/` — review once to identify the large unexercised regions (likely: admin endpoints, error-handling branches, edge-case controllers).
- Do **not** add a build-failing threshold yet — treat the first report as a baseline, then agree on a ratchet (e.g., "new code in this PR must be ≥80% covered") before adding a quality gate.
- Optional follow-up: wire the report into a CI artifact so it's viewable from each PR.

**Caveats:**

- Coverage numbers are easy to game. High line coverage with weak assertions is worse than honest lower coverage — PIT is the corrective.
- Integration tests that hit the DB will produce different coverage shapes than unit tests would; that's fine, but keep it in mind when reading the report.

### How A and B complement each other

- **JaCoCo shows code that's never executed by any test** — you can't mutation-test code nobody runs.
- **PIT shows code that's executed but not meaningfully asserted** — the stronger signal.
- Adopted together, a "well-tested" class means high JaCoCo coverage **and** high PIT mutation score.

### Out of scope for this phase

Frontend coverage (Vitest `v8`), frontend mutation testing (Stryker), security scanning (Semgrep, OWASP Dependency-Check), contract testing (Pact), and visual regression — all candidates for later phases. See [DEFERRED_WORK.md](DEFERRED_WORK.md) if/when captured.

## Phase: Admin panel authentication & authorization

**Motivation:** Admin endpoints (`/api/v1/admin/**`) are currently `.permitAll()` in `SecurityConfig` — no authentication, no role check. Any anonymous request can CRUD all users, projects, tasks, deferred items, reflections, and time blocks. This is a development convenience that **must not ship to production**, and even in local dev stray requests can mutate the database. This phase lands baseline authentication plus role-based access.

**Priority:** High — must be resolved before any production deployment. Arguably should precede the testing-thoroughness phase.

### Tasks

- **Add a role field to `AppUser`.** New column (`role` VARCHAR, NOT NULL, DEFAULT `'USER'`, CHECK constraint on `('USER', 'ADMIN')`) via a Flyway migration. Existing users default to `USER`.
- **Seed an admin user** in a dev-only Flyway migration (e.g., a `V<N>__seed_admin_user.sql` gated to non-production profiles, or a `CommandLineRunner` that only runs under the `dev` profile). Document the credentials in `CONTRIBUTING.md` so other devs can exercise admin flows.
- **Surface the role in JWT claims** so `SecurityConfig` can authorize without an extra DB lookup per request. Update `JwtService` (or equivalent) to embed `role`, and the authentication filter to populate `SimpleGrantedAuthority("ROLE_ADMIN")` when present.
- **Update `SecurityConfig`** — change `/api/v1/admin/**` from `.permitAll()` to `.hasRole("ADMIN")`. Confirm the existing JWT filter chain covers this path.
- **Frontend handling** — the admin panel pages must gracefully handle 401/403 responses (redirect to login or show an access-denied message), not silently fail. Hide admin nav links from non-admin users.
- **Integration tests** covering: anonymous request to `/api/v1/admin/**` returns 401; authenticated `USER` returns 403; authenticated `ADMIN` succeeds. These tests also become early PIT targets once Phase: Verifiable test thoroughness lands.

### Caveats

- **Don't ship the dev admin seed to production.** Gate by Spring profile or skip the seed migration in prod. A production admin user should be created manually via a one-off script or existing user promotion flow.
- **Consider a promotion path** — how does a regular user become an admin? Out of scope for this phase (initial admin is seeded), but worth capturing as follow-up work if the product grows beyond a single operator.
- **Rotating JWT secrets after adding role claims** invalidates all outstanding tokens, forcing re-login. Fine in dev; worth coordinating if any real users exist at rollout.
