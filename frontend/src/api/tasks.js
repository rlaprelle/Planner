import { authFetch } from './client'

const BASE = '/api/v1'

async function handleResponse(res) {
  if (!res.ok) {
    let message = `HTTP ${res.status}`
    try {
      const body = await res.json()
      message = body.message || body.error || message
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

export async function getProjectTasks(projectId) {
  const res = await authFetch(`${BASE}/projects/${projectId}/tasks`)
  return handleResponse(res)
}

export async function createTask(projectId, data) {
  const res = await authFetch(`${BASE}/projects/${projectId}/tasks`, {
    method: 'POST',
    body: JSON.stringify(data),
  })
  return handleResponse(res)
}

export async function getTask(taskId) {
  const res = await authFetch(`${BASE}/tasks/${taskId}`)
  return handleResponse(res)
}

export async function updateTask(taskId, data) {
  const res = await authFetch(`${BASE}/tasks/${taskId}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  })
  return handleResponse(res)
}

export async function archiveTask(taskId) {
  const res = await authFetch(`${BASE}/tasks/${taskId}/archive`, {
    method: 'PATCH',
  })
  return handleResponse(res)
}

export async function updateTaskStatus(taskId, status) {
  const res = await authFetch(`${BASE}/tasks/${taskId}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  })
  return handleResponse(res)
}
