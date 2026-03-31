#Requires -Version 5.1

$ErrorActionPreference = "Stop"

# ── constants ────────────────────────────────────────────────────────────────
$ScriptDir              = Split-Path -Parent $MyInvocation.MyCommand.Definition
$BackendLog             = "$ScriptDir\backend.log"
$BackendErrorLog        = "$ScriptDir\backend-error.log"
$ComposeFile            = "$ScriptDir\docker-compose.yml"
$DatabaseTimeoutSeconds = 30
$BackendTimeoutSeconds  = 60
$HealthPollSeconds      = 2
$BackendPort            = 8080
$FrontendPort           = 5173
$LogTailLines           = 20

# ── output helpers ───────────────────────────────────────────────────────────
function ok   { param($msg) Write-Host "checkmark $msg" -ForegroundColor Green }
function info { param($msg) Write-Host ">> $msg" -ForegroundColor Yellow }
function fail { param($msg) Write-Host "!! $msg" -ForegroundColor Red; exit 1 }

# ── functions ────────────────────────────────────────────────────────────────

function Find-Maven {
    if (Get-Command mvn -ErrorAction SilentlyContinue) { return "mvn" }

    $wrapperBase = "$env:USERPROFILE\.m2\wrapper\dists"
    $found = Get-ChildItem -Path $wrapperBase -Recurse -Filter "mvn.cmd" -ErrorAction SilentlyContinue |
             Select-Object -First 1 -ExpandProperty FullName
    if ($found) { return $found }

    fail "Maven not found. Install it or add it to PATH."
}

function Start-Database {
    info "Starting database..."
    & docker compose -f $ComposeFile up -d
    if ($LASTEXITCODE -ne 0) { fail "docker compose up failed" }

    info "Waiting for PostgreSQL..."
    for ($i = 0; $i -lt $DatabaseTimeoutSeconds; $i++) {
        $health = docker inspect planner-db --format "{{.State.Health.Status}}" 2>$null
        if ($health -eq "healthy") {
            ok "Database is ready"
            return
        }
        Start-Sleep -Seconds 1
    }
    fail "Database did not become ready in time"
}

function Test-BackendHealth {
    try {
        $r = Invoke-RestMethod -Uri "http://localhost:${BackendPort}/actuator/health" -TimeoutSec $HealthPollSeconds
        return ($r.status -eq "UP")
    } catch {
        return $false
    }
}

function Show-BackendLogs {
    param($prefix)
    Write-Host "$prefix Last $LogTailLines lines of backend.log:" -ForegroundColor Red
    if (Test-Path $BackendLog) { Get-Content $BackendLog -Tail $LogTailLines }
}

function Start-Backend {
    param($mvn)

    info "Starting backend (logs -> backend.log)..."
    $script:BackendProcess = Start-Process `
        -FilePath $mvn `
        -ArgumentList "spring-boot:run" `
        -WorkingDirectory "$ScriptDir\backend" `
        -RedirectStandardOutput $BackendLog `
        -RedirectStandardError  $BackendErrorLog `
        -NoNewWindow -PassThru

    info "Waiting for backend..."
    for ($i = 0; $i -lt $BackendTimeoutSeconds; $i++) {
        if ($script:BackendProcess.HasExited) {
            Show-BackendLogs "Backend crashed."
            fail "Backend failed to start"
        }
        if (Test-BackendHealth) {
            ok "Backend is ready  ->  http://localhost:${BackendPort}"
            return
        }
        Start-Sleep -Seconds $HealthPollSeconds
    }
    Show-BackendLogs "Timed out."
    fail "Backend did not become ready in time"
}

function Start-Frontend {
    info "Starting frontend..."
    Set-Location "$ScriptDir\frontend"
    if (-not (Test-Path "node_modules")) {
        info "Installing frontend dependencies..."
        & npm install
        if ($LASTEXITCODE -ne 0) { fail "npm install failed" }
    }
    ok "Frontend starting  ->  http://localhost:${FrontendPort}"
    Write-Host ""
    Write-Host "All services running. Press Ctrl+C to stop everything." -ForegroundColor Green
    Write-Host ""
    & npm run dev
}

function Stop-All {
    Write-Host ""
    info "Shutting down..."
    if ($null -ne $script:BackendProcess -and -not $script:BackendProcess.HasExited) {
        taskkill /F /T /PID $script:BackendProcess.Id 2>&1 | Out-Null
        ok "Backend stopped"
    }
    $prev = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    docker compose -f $ComposeFile stop 2>&1 | Out-Null
    $ErrorActionPreference = $prev
    ok "Database stopped"
}

# ── main ─────────────────────────────────────────────────────────────────────
$script:BackendProcess = $null
$mvn = Find-Maven

try {
    Start-Database
    Start-Backend $mvn
    Start-Frontend
} finally {
    Stop-All
}
