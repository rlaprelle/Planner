export const ADMIN_USERS = [
  { id: 'u1', email: 'alice@example.com', displayName: 'Alice', timezone: 'UTC', role: 'ADMIN', createdAt: '2026-03-15T10:00:00Z', updatedAt: '2026-03-15T10:00:00Z' },
  { id: 'u2', email: 'bob@example.com', displayName: 'Bob', timezone: 'America/New_York', role: 'USER', createdAt: '2026-03-20T10:00:00Z', updatedAt: '2026-03-20T10:00:00Z' },
]

// JWT payload {"sub":"u1","role":"ADMIN"} — the frontend decodes but does not verify,
// so any three-segment base64url string with a valid payload works.
export const ADMIN_JWT = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1MSIsInJvbGUiOiJBRE1JTiJ9.ZmFrZQ'

export const ADMIN_PROJECTS = [
  { id: 'p1', userId: 'u1', userEmail: 'alice@example.com', name: 'Work', description: null, color: '#6b4c9a', icon: null, isActive: true, sortOrder: 0, archivedAt: null, createdAt: '2026-03-15T10:00:00Z', updatedAt: '2026-03-15T10:00:00Z' },
]

export const ADMIN_TASKS = [
  { id: 't1', userId: 'u1', userEmail: 'alice@example.com', projectId: 'p1', projectName: 'Work', title: 'Fix bug', description: null, parentTaskId: null, status: 'TODO', priority: 3, pointsEstimate: null, actualMinutes: null, energyLevel: null, dueDate: null, sortOrder: 0, blockedByTaskId: null, archivedAt: null, completedAt: null, createdAt: '2026-03-15T10:00:00Z', updatedAt: '2026-03-15T10:00:00Z' },
]

export const ADMIN_DEFERRED: any[] = []
export const ADMIN_REFLECTIONS: any[] = []
export const ADMIN_TIME_BLOCKS: any[] = []

export const USER_DEPENDENTS = { projects: 1, tasks: 1, deferredItems: 0, reflections: 0, timeBlocks: 0 }
