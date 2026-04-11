#!/usr/bin/env node

/**
 * Dev server manager for multi-worktree development.
 *
 * Usage:
 *   node dev.js start    — Start a dev server (or report existing one)
 *   node dev.js status   — Show all running dev servers across worktrees
 *   node dev.js stop     — Stop the dev server for this worktree
 */

import { createServer, connect } from 'net'
import { spawn, execSync } from 'child_process'
import { readFileSync, writeFileSync, unlinkSync, readdirSync, existsSync } from 'fs'
import { join, resolve } from 'path'
import { createHash } from 'crypto'

const PORT_FILE = '.dev-port'
const WORKTREE_DIR = join('.claude', 'worktrees')
const DEFAULT_PORT = 5173
const WORKTREE_PORT_MIN = 5200
const WORKTREE_PORT_MAX = 5299

/** Detect whether we're in the main checkout or a worktree. */
function getProjectRoot() {
  return resolve('.')
}

function isWorktree() {
  try {
    const gitFile = readFileSync('.git', 'utf8').trim()
    return gitFile.startsWith('gitdir:')
  } catch {
    return false
  }
}

/** Deterministic port from worktree path. Main checkout gets 5173. */
function computePort(projectRoot) {
  if (!isWorktree()) return DEFAULT_PORT
  const hash = createHash('md5').update(projectRoot).digest()
  const range = WORKTREE_PORT_MAX - WORKTREE_PORT_MIN + 1
  return WORKTREE_PORT_MIN + (hash.readUInt16LE(0) % range)
}

/** Check if a port has something listening on it. Returns true if in use. */
function isPortInUse(port) {
  // Attempt a TCP connect to detect listeners on any interface.
  // Vite binds to ::1 (IPv6 loopback) on Windows, so a listen-based
  // check on 0.0.0.0 gives false negatives.
  return new Promise((resolve) => {
    const socket = connect({ port, host: 'localhost' })
    socket.once('connect', () => {
      socket.destroy()
      resolve(true)    // something is listening
    })
    socket.once('error', () => {
      socket.destroy()
      resolve(false)   // nothing listening
    })
  })
}

/** Read the .dev-port file, return port number or null. */
function readPortFile(dir) {
  const filePath = join(dir, PORT_FILE)
  try {
    const content = readFileSync(filePath, 'utf8').trim()
    const port = parseInt(content, 10)
    return isNaN(port) ? null : port
  } catch {
    return null
  }
}

/** Find the main repo root by looking for .claude/worktrees from cwd upward. */
function findRepoRoot() {
  // If we're in a worktree, the main repo is the parent of .claude/worktrees/<name>
  if (isWorktree()) {
    try {
      const gitContent = readFileSync('.git', 'utf8').trim()
      // gitdir: /path/to/repo/.git/worktrees/<name>
      const match = gitContent.match(/gitdir:\s*(.+)/)
      if (match) {
        const gitWorktreePath = match[1].replace(/\\/g, '/')
        const repoGitDir = gitWorktreePath.replace(/\/worktrees\/[^/]+$/, '')
        return resolve(repoGitDir, '..')
      }
    } catch { /* fall through */ }
  }
  return resolve('.')
}

// --- Commands ---

async function start() {
  const projectRoot = getProjectRoot()
  const savedPort = readPortFile(projectRoot)

  // Check if there's already a server running
  if (savedPort !== null) {
    const alive = await isPortInUse(savedPort)
    if (alive) {
      console.log(`Already running on port ${savedPort}`)
      console.log(`  http://localhost:${savedPort}`)
      return
    }
    // Stale file — clean up
    try { unlinkSync(join(projectRoot, PORT_FILE)) } catch { /* ignore */ }
  }

  // Compute the port for this worktree
  let port = computePort(projectRoot)

  // Check if the computed port is free, find next available if not
  let inUse = await isPortInUse(port)
  if (inUse) {
    const originalPort = port
    for (let p = WORKTREE_PORT_MIN; p <= WORKTREE_PORT_MAX; p++) {
      if (p === originalPort) continue
      if (!(await isPortInUse(p))) {
        port = p
        inUse = false
        break
      }
    }
    if (inUse && !isWorktree()) {
      // Main checkout — try 5173 range
      console.error(`No free port found in range ${WORKTREE_PORT_MIN}-${WORKTREE_PORT_MAX}`)
      process.exit(1)
    }
  }

  // Start vite
  const frontendDir = join(projectRoot, 'frontend')
  if (!existsSync(frontendDir)) {
    console.error('frontend/ directory not found. Run from the project root or a worktree.')
    process.exit(1)
  }

  // Install deps if needed
  if (!existsSync(join(frontendDir, 'node_modules'))) {
    console.log('Installing frontend dependencies...')
    execSync('npm install', { cwd: frontendDir, stdio: 'inherit' })
  }

  console.log(`Starting dev server on port ${port}...`)

  // Spawn vite as a detached process. Run vite's bin entry directly via Node
  // to avoid shell/npx/.cmd issues on Windows with detached processes.
  const viteBin = join(frontendDir, 'node_modules', 'vite', 'bin', 'vite.js')
  const child = spawn(process.execPath, [viteBin, '--port', String(port), '--strictPort'], {
    cwd: frontendDir,
    stdio: 'ignore',
    detached: true,
  })
  child.unref()

  // Wait briefly for server to start
  let started = false
  for (let i = 0; i < 20; i++) {
    await new Promise((r) => setTimeout(r, 500))
    if (await isPortInUse(port)) {
      started = true
      break
    }
  }

  if (!started) {
    console.error(`Server failed to start on port ${port} within 10 seconds`)
    process.exit(1)
  }

  // Write port file
  writeFileSync(join(projectRoot, PORT_FILE), String(port) + '\n')

  console.log(`Dev server running on port ${port}`)
  console.log(`  http://localhost:${port}`)
}

async function status() {
  const repoRoot = findRepoRoot()
  const entries = []

  // Check main checkout
  const mainPort = readPortFile(repoRoot)
  if (mainPort !== null) {
    const alive = await isPortInUse(mainPort)
    entries.push({ name: 'main', dir: repoRoot, port: mainPort, alive })
    if (!alive) {
      try { unlinkSync(join(repoRoot, PORT_FILE)) } catch { /* ignore */ }
    }
  }

  // Check all worktrees
  const worktreeBase = join(repoRoot, WORKTREE_DIR)
  if (existsSync(worktreeBase)) {
    try {
      const dirs = readdirSync(worktreeBase, { withFileTypes: true })
        .filter((d) => d.isDirectory())
        .map((d) => d.name)

      for (const name of dirs) {
        const wtDir = join(worktreeBase, name)
        const port = readPortFile(wtDir)
        if (port !== null) {
          const alive = await isPortInUse(port)
          entries.push({ name, dir: wtDir, port, alive })
          if (!alive) {
            try { unlinkSync(join(wtDir, PORT_FILE)) } catch { /* ignore */ }
          }
        }
      }
    } catch { /* worktree dir doesn't exist */ }
  }

  if (entries.length === 0) {
    console.log('No dev servers tracked.')
    return
  }

  console.log('Dev servers:')
  console.log()
  for (const entry of entries) {
    const status = entry.alive ? `\x1b[32mrunning\x1b[0m` : `\x1b[31mstopped (cleaned up)\x1b[0m`
    console.log(`  ${entry.name.padEnd(30)} port ${entry.port}  ${status}`)
  }
}

async function stop() {
  const projectRoot = getProjectRoot()
  const port = readPortFile(projectRoot)

  if (port === null) {
    console.log('No dev server tracked for this directory.')
    return
  }

  const alive = await isPortInUse(port)
  if (!alive) {
    console.log(`No server running on port ${port} (cleaning up stale file).`)
    try { unlinkSync(join(projectRoot, PORT_FILE)) } catch { /* ignore */ }
    return
  }

  // Kill the process on the port
  try {
    if (process.platform === 'win32') {
      // Find PID using netstat, then kill
      const output = execSync(`netstat -ano | findstr :${port} | findstr LISTENING`, { encoding: 'utf8' })
      const lines = output.trim().split('\n')
      const pids = new Set()
      for (const line of lines) {
        const parts = line.trim().split(/\s+/)
        const pid = parts[parts.length - 1]
        if (pid && pid !== '0') pids.add(pid)
      }
      for (const pid of pids) {
        try { execSync(`taskkill /F /PID ${pid}`, { stdio: 'ignore' }) } catch { /* ignore */ }
      }
    } else {
      execSync(`lsof -ti:${port} | xargs kill -9`, { stdio: 'ignore' })
    }
  } catch {
    console.error(`Could not kill process on port ${port}.`)
  }

  try { unlinkSync(join(projectRoot, PORT_FILE)) } catch { /* ignore */ }
  console.log(`Stopped dev server on port ${port}.`)
}

// --- Main ---

const command = process.argv[2]

switch (command) {
  case 'start':
    start()
    break
  case 'status':
    status()
    break
  case 'stop':
    stop()
    break
  default:
    console.log('Usage: node dev.js <start|status|stop>')
    console.log()
    console.log('  start   — Start a dev server (or report existing one)')
    console.log('  status  — Show all running dev servers across worktrees')
    console.log('  stop    — Stop the dev server for this worktree')
    process.exit(1)
}
