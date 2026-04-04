const BASE = '/api/v1/admin'

async function handleResponse(res) {
  if (!res.ok) {
    let message = `HTTP ${res.status}`
    try {
      const body = await res.json()
      message = body.detail || body.message || body.error || message
    } catch {
      // ignore parse errors
    }
    const err = new Error(message)
    err.status = res.status
    throw err
  }
  if (res.status === 204) return null
  return res.json()
}

function adminFetch(path, options = {}) {
  return fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json', ...options.headers },
    ...options,
  })
}

// Users
export async function getUsers() {
  return handleResponse(await adminFetch('/users'))
}
export async function getUser(id) {
  return handleResponse(await adminFetch(`/users/${id}`))
}
export async function createUser(data) {
  return handleResponse(await adminFetch('/users', { method: 'POST', body: JSON.stringify(data) }))
}
export async function updateUser(id, data) {
  return handleResponse(await adminFetch(`/users/${id}`, { method: 'PUT', body: JSON.stringify(data) }))
}
export async function getUserDependents(id) {
  return handleResponse(await adminFetch(`/users/${id}/dependents`))
}
export async function deleteUser(id) {
  return handleResponse(await adminFetch(`/users/${id}`, { method: 'DELETE' }))
}

// Projects
export async function getProjects() {
  return handleResponse(await adminFetch('/projects'))
}
export async function createProject(data) {
  return handleResponse(await adminFetch('/projects', { method: 'POST', body: JSON.stringify(data) }))
}
export async function updateProject(id, data) {
  return handleResponse(await adminFetch(`/projects/${id}`, { method: 'PUT', body: JSON.stringify(data) }))
}
export async function deleteProject(id) {
  return handleResponse(await adminFetch(`/projects/${id}`, { method: 'DELETE' }))
}

// Tasks
export async function getTasks() {
  return handleResponse(await adminFetch('/tasks'))
}
export async function createTask(data) {
  return handleResponse(await adminFetch('/tasks', { method: 'POST', body: JSON.stringify(data) }))
}
export async function updateTask(id, data) {
  return handleResponse(await adminFetch(`/tasks/${id}`, { method: 'PUT', body: JSON.stringify(data) }))
}
export async function deleteTask(id) {
  return handleResponse(await adminFetch(`/tasks/${id}`, { method: 'DELETE' }))
}

// Deferred Items
export async function getDeferredItems() {
  return handleResponse(await adminFetch('/deferred-items'))
}
export async function createDeferredItem(data) {
  return handleResponse(await adminFetch('/deferred-items', { method: 'POST', body: JSON.stringify(data) }))
}
export async function updateDeferredItem(id, data) {
  return handleResponse(await adminFetch(`/deferred-items/${id}`, { method: 'PUT', body: JSON.stringify(data) }))
}
export async function deleteDeferredItem(id) {
  return handleResponse(await adminFetch(`/deferred-items/${id}`, { method: 'DELETE' }))
}

// Reflections
export async function getReflections() {
  return handleResponse(await adminFetch('/reflections'))
}
export async function createReflection(data) {
  return handleResponse(await adminFetch('/reflections', { method: 'POST', body: JSON.stringify(data) }))
}
export async function updateReflection(id, data) {
  return handleResponse(await adminFetch(`/reflections/${id}`, { method: 'PUT', body: JSON.stringify(data) }))
}
export async function deleteReflection(id) {
  return handleResponse(await adminFetch(`/reflections/${id}`, { method: 'DELETE' }))
}

// Events
export async function getEvents() {
  return handleResponse(await adminFetch('/events'))
}
export async function createAdminEvent(data) {
  return handleResponse(await adminFetch('/events', { method: 'POST', body: JSON.stringify(data) }))
}
export async function updateAdminEvent(id, data) {
  return handleResponse(await adminFetch(`/events/${id}`, { method: 'PUT', body: JSON.stringify(data) }))
}
export async function deleteAdminEvent(id) {
  return handleResponse(await adminFetch(`/events/${id}`, { method: 'DELETE' }))
}

// Time Blocks
export async function getTimeBlocks() {
  return handleResponse(await adminFetch('/time-blocks'))
}
export async function createTimeBlock(data) {
  return handleResponse(await adminFetch('/time-blocks', { method: 'POST', body: JSON.stringify(data) }))
}
export async function updateTimeBlock(id, data) {
  return handleResponse(await adminFetch(`/time-blocks/${id}`, { method: 'PUT', body: JSON.stringify(data) }))
}
export async function deleteTimeBlock(id) {
  return handleResponse(await adminFetch(`/time-blocks/${id}`, { method: 'DELETE' }))
}
