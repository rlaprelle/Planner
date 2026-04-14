# Contributing to Echel Planner

> **Purpose:** Development environment setup, dependencies, testing commands, environment config.
> **Audience:** Humans setting up a development environment.
> **Out of scope:** Product description and design rationale — see [README.md](README.md). Architecture and data model — see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Development Setup

### Prerequisites

- Java 21
- Maven 3.8+
- Node.js 20+
- Docker Desktop

### Start Everything

**Git Bash / WSL:**
```bash
./start.sh
```

**PowerShell:**
```powershell
.\start.ps1
```

Both scripts start the database, wait for it to be ready, start the backend, wait for the health check, then launch the frontend. Press **Ctrl+C** to stop everything cleanly.

Open **http://localhost:5173** — you'll be redirected to `/login`.

Backend logs are written to `backend.log` (stdout) and `backend-error.log` (stderr) in the project root.

<details>
<summary>Manual startup (if you prefer separate terminals)</summary>

**1. Database:**
```bash
docker compose up
```

**2. Backend:**
```bash
cd backend
mvn spring-boot:run
```

Verify it's up: `curl http://localhost:8080/actuator/health` should return `{"status":"UP"}`.

**3. Frontend:**
```bash
cd frontend
npm install   # first time only
npm run dev
```
</details>

#### Dev Server Manager

When working in multiple git worktrees simultaneously, use `dev.js` to manage frontend dev servers without port conflicts:

```bash
node dev.js start    # Start a dev server (or see which one is already running)
node dev.js status   # See all running dev servers across worktrees
node dev.js stop     # Stop the dev server for this worktree
```

Each worktree gets a deterministic port (5200-5299 range). The main checkout uses port 5173.

### Environment Variables

All defaults work for local dev out of the box. Override as needed:

| Variable | Default | Description |
|----------|---------|-------------|
| `PLANNER_DB_URL` | `jdbc:postgresql://localhost:5432/planner` | Database URL |
| `PLANNER_DB_USER` | `planner` | Database username |
| `PLANNER_DB_PASSWORD` | `planner` | Database password |
| `JWT_SECRET` | (built-in dev secret) | JWT signing key — **set this in production** |

## Testing

```bash
# Backend integration tests (require live PostgreSQL)
cd backend && mvn test

# Frontend linting
cd frontend && npm run lint

# E2E tests (all API calls mocked — just needs Vite running)
cd e2e && npx playwright test
```

### E2E Test Details

- E2E tests mock all `/api/*` calls via `page.route()` — no backend required, just Vite.
- Use `node dev.js start` from the project root to start a Vite server, then pass its port: `cd e2e && BASE_URL=http://localhost:<port> npx playwright test`.
- Do not kill other Vite servers — multiple worktrees may be running concurrently.

## Worktree Caveats

All worktrees share a single PostgreSQL container (`planner-db`) and database. Flyway migrations applied by one branch affect all others. If a worktree applies a new migration (e.g., adds columns), an older branch's backend may fail with Hibernate schema validation errors. The start scripts detect this and suggest `docker compose -p planner down -v` to reset.

## Architecture & Project Structure

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the data model, API endpoints, tech stack, and project layout.
