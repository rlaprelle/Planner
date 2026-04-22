#!/usr/bin/env bash
set -euo pipefail

# ── constants ────────────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel 2>/dev/null || echo "$SCRIPT_DIR")"
COMPOSE_FILE="$REPO_ROOT/docker-compose.yml"
COMPOSE_PROJECT_NAME="planner"
BACKEND_LOG="$SCRIPT_DIR/backend.log"
DATABASE_TIMEOUT=30
BACKEND_TIMEOUT=60
HEALTH_POLL_SECONDS=2
BACKEND_PORT=8080
FRONTEND_PORT=5173
LOG_TAIL_LINES=20

# ── output helpers ───────────────────────────────────────────────────────────
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
ok()   { echo -e "${GREEN}✓ $*${NC}"; }
info() { echo -e "${YELLOW}▶ $*${NC}"; }
fail() { echo -e "${RED}✗ $*${NC}"; exit 1; }

# ── functions ────────────────────────────────────────────────────────────────

find_maven() {
  if command -v mvn &>/dev/null; then
    echo "mvn"
    return
  fi

  local m2_wrapper="$HOME/.m2/wrapper/dists"
  local found
  found=$(find "$m2_wrapper" -name "mvn" -not -name "*.cmd" 2>/dev/null | head -1)
  [[ -n "$found" ]] || fail "Maven not found. Install it or add it to PATH."
  echo "$found"
}

start_database() {
  # If the container already exists (e.g. from another worktree), reuse it
  local state
  state=$(docker inspect planner-db --format "{{.State.Status}}" 2>/dev/null || echo "")
  if [[ "$state" == "running" ]]; then
    local health
    health=$(docker inspect planner-db --format "{{.State.Health.Status}}" 2>/dev/null || echo "")
    if [[ "$health" == "healthy" ]]; then
      ok "Database already running"
      return
    fi
    info "Database container running but not healthy yet, waiting..."
  elif [[ -n "$state" ]]; then
    # Container exists but is stopped/exited — start it directly
    info "Starting existing database container..."
    docker start planner-db
  else
    # No container exists — create it via compose
    info "Starting database..."
    docker compose -p "$COMPOSE_PROJECT_NAME" -f "$COMPOSE_FILE" up -d
  fi

  info "Waiting for PostgreSQL to be ready..."
  for i in $(seq 1 "$DATABASE_TIMEOUT"); do
    local health
    health=$(docker inspect planner-db --format "{{.State.Health.Status}}" 2>/dev/null || echo "")
    if [[ "$health" == "healthy" ]]; then
      ok "Database is ready"
      return
    fi
    [[ $i -eq $DATABASE_TIMEOUT ]] && fail "Database did not become ready in time"
    sleep 1
  done
}

is_backend_healthy() {
  curl -sf "http://localhost:${BACKEND_PORT}/actuator/health" | grep -q '"UP"'
}

assert_backend_running() {
  kill -0 "$BACKEND_PID" 2>/dev/null && return
  show_backend_logs "Backend crashed."
  fail "Backend failed to start"
}

show_backend_logs() {
  local prefix="$1"
  echo ""
  echo "$prefix Last $LOG_TAIL_LINES lines of backend.log:"
  tail -"$LOG_TAIL_LINES" "$BACKEND_LOG"
  # Check for schema mismatch (shared DB across worktrees with different migrations)
  if grep -qiE "Schema-validation|FlywayValidateException|Missing column|wrong column type" "$BACKEND_LOG" 2>/dev/null; then
    echo ""
    echo -e "${RED}!! This looks like a database schema mismatch.${NC}"
    echo -e "${RED}!! The database was likely migrated by a different branch/worktree.${NC}"
    echo -e "${RED}!! To fix: stop the DB, delete the volume, and restart:${NC}"
    echo -e "${YELLOW}!!   docker compose -p planner down -v${NC}"
    echo -e "${YELLOW}!!   Then re-run this script.${NC}"
    echo ""
  fi
}

start_backend() {
  local mvn="$1"

  info "Starting backend (logs → backend.log)..."
  cd "$SCRIPT_DIR/backend"
  # Activate the dev profile so DevAdminSeeder creates the local admin user.
  # Production deployments must not set this profile.
  "$mvn" spring-boot:run -Dspring-boot.run.profiles=dev > "$BACKEND_LOG" 2>&1 &
  BACKEND_PID=$!

  info "Waiting for backend to be ready..."
  for i in $(seq 1 "$BACKEND_TIMEOUT"); do
    if is_backend_healthy; then
      ok "Backend is ready  →  http://localhost:${BACKEND_PORT}"
      return
    fi
    assert_backend_running
    [[ $i -eq $BACKEND_TIMEOUT ]] && {
      show_backend_logs "Timed out."
      fail "Backend did not become ready in time"
    }
    sleep "$HEALTH_POLL_SECONDS"
  done
}

start_frontend() {
  info "Starting frontend..."
  cd "$SCRIPT_DIR/frontend"
  [[ -d node_modules ]] || { info "Installing frontend dependencies..."; npm install; }
  ok "Frontend starting  →  http://localhost:${FRONTEND_PORT}"
  echo ""
  echo -e "${GREEN}All services running. Press Ctrl+C to stop everything.${NC}"
  echo ""
  npm run dev
}

# ── cleanup ──────────────────────────────────────────────────────────────────
BACKEND_PID=""
cleanup() {
  echo ""
  info "Shutting down..."
  [[ -n "$BACKEND_PID" ]] && kill "$BACKEND_PID" 2>/dev/null && ok "Backend stopped"
  docker compose -p "$COMPOSE_PROJECT_NAME" -f "$COMPOSE_FILE" stop 2>/dev/null && ok "Database stopped"
  exit 0
}
trap cleanup SIGINT SIGTERM

# ── main ─────────────────────────────────────────────────────────────────────
MVN=$(find_maven)
start_database
start_backend "$MVN"
start_frontend
