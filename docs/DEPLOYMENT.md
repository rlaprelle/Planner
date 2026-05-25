# Deployment

Operational notes for getting Echel Planner in front of real users. Covers TLS, the prod Spring profile, and the startup guards that fail fast on misconfiguration.

## Deploy shapes

**Primary — AWS App Runner.** See [#88](https://github.com/rlaprelle/Planner/issues/88) for the pipeline (ECR push from CI → App Runner deploy). App Runner provides a managed HTTPS endpoint and terminates TLS at its edge automatically; the app container speaks plain HTTP behind it. No proxy work for the operator.

**Secondary — self-host via `compose.deploy.yml`.** See [#77](https://github.com/rlaprelle/Planner/issues/77) for the compose file. This shape ships only the app + database. The operator is responsible for putting a TLS-terminating reverse proxy (Caddy, nginx, or Traefik) in front of the backend container and forwarding `X-Forwarded-Proto: https`. We do not ship a proxy.

## TLS termination

Both deploy shapes terminate TLS upstream of the Spring Boot app. The prod profile sets:

```properties
server.forward-headers-strategy=framework
```

This tells Spring to honor `X-Forwarded-Proto` from the proxy / App Runner, so `HttpServletRequest.isSecure()` returns `true` for forwarded HTTPS requests.

**Why this matters.** Refresh-token cookies are issued with `Secure; HttpOnly; SameSite=Strict`. The `Secure` attribute tells the browser to send the cookie only over HTTPS. If Spring sees the inbound request as plaintext HTTP (because forward headers are not honored), it will issue cookies under a non-secure context, browsers will drop the cookie at the next round-trip, and authentication silently breaks. Server logs show successful 200s; only the client-side missing cookie reveals the problem.

## Startup guard

`HttpsTerminationGuard` (in `backend/src/main/java/com/echel/planner/backend/common/`) runs under the `prod` profile only. It fails application startup with `IllegalStateException` unless one of the following is configured:

- `server.forward-headers-strategy` is set (typical for App Runner / nginx / Caddy / Traefik), **or**
- `server.ssl.enabled=true` (Spring terminating TLS itself — uncommon for this app but allowed).

If neither is set, the app refuses to start rather than running silently misconfigured.

Companion guard: `JwtSecretProductionGuard` (see [#79](https://github.com/rlaprelle/Planner/issues/79)) fails startup if the dev-default JWT secret leaks into prod.

## Activating the prod profile

Set `SPRING_PROFILES_ACTIVE=prod` in the environment. App Runner sets this via the service configuration; for self-host, pass it via `compose.deploy.yml`'s `environment:` block.

Required env vars under prod (no localhost / dev defaults):

- `JWT_SECRET` — 256-bit secret (no default; placeholder resolution fails startup if unset)
- `PLANNER_DB_URL`, `PLANNER_DB_USER`, `PLANNER_DB_PASSWORD` — Postgres connection (no defaults)

## Rollback

For App Runner: re-trigger a deploy of the previous image SHA. The CI workflow tags images with `:sha-<git-sha>`, so any prior green build is redeployable.

For self-host: redeploy the previous `:sha-<git-sha>` tag from ECR (or the local registry). Flyway is forward-only; if the previous version expects an older schema, plan the database revert separately — Flyway does not undo migrations.
