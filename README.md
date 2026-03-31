# Planner

A daily mindfulness and work management tool designed for ADHD brains.

## Getting Started

### Prerequisites

- Java 21
- Maven 3.8+
- Node.js 20+
- Docker Desktop

### Start everything

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

Verify it's up: `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`

**3. Frontend:**
```bash
cd frontend
npm install   # first time only
npm run dev
```
</details>

### Environment variables

All defaults work for local dev out of the box. To override:

| Variable | Default | Description |
|----------|---------|-------------|
| `PLANNER_DB_URL` | `jdbc:postgresql://localhost:5432/planner` | Database URL |
| `PLANNER_DB_USER` | `planner` | Database username |
| `PLANNER_DB_PASSWORD` | `planner` | Database password |
| `JWT_SECRET` | (built-in dev secret) | JWT signing key — **set this in production** |

---

## Documentation

### Architecture & Implementation
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) — Data model, API endpoints, tech stack
- [`docs/IMPLEMENTATION_PLAN.md`](docs/IMPLEMENTATION_PLAN.md) — Design decisions, implementation plan
- [`docs/planning/2026-03-30-implementation-plan-design.md`](docs/planning/2026-03-30-implementation-plan-design.md) — Detailed vertical slice spec (Slices 0-6)

### User Design Documents
- [`docs/planning/user_design/USER_WISHES.md`](docs/planning/user_design/USER_WISHES.md) — Original vision
- [`docs/planning/user_design/USE_CASES.md`](docs/planning/user_design/USE_CASES.md) — 5 core use cases
- [`docs/planning/user_design/CORE_WORKFLOWS.md`](docs/planning/user_design/CORE_WORKFLOWS.md) — 6 views, detailed flows
- [`docs/planning/user_design/INFORMATION_ARCHITECTURE.md`](docs/planning/user_design/INFORMATION_ARCHITECTURE.md) — Entity definitions, task ordering
- [`docs/planning/user_design/WIREFRAMES.md`](docs/planning/user_design/WIREFRAMES.md) — ASCII wireframes
- [`docs/planning/user_design/DEFERRED_WORK.md`](docs/planning/user_design/DEFERRED_WORK.md) — Phase 2+ features
- [`docs/planning/user_design/USER_DESIGN_CHECKLIST.md`](docs/planning/user_design/USER_DESIGN_CHECKLIST.md) — Design phase checklist
