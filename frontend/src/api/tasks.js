import { authFetch, handleResponse } from './client'

const BASE = '/api/v1'

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

export async function getTodayCompletedTasks() {
  const res = await authFetch(`/api/v1/tasks/completed-today`)
  return handleResponse(res)
}

export async function getActiveTasks() {
  const res = await authFetch(`${BASE}/tasks/active`)
  return handleResponse(res)
}

export async function deferTask(taskId, target) {
  const res = await authFetch(`${BASE}/tasks/${taskId}/defer`, {
    method: 'PATCH',
    body: JSON.stringify({ target }),
  })
  return handleResponse(res)
}

export async function cancelTask(taskId) {
  const res = await authFetch(`${BASE}/tasks/${taskId}/cancel`, {
    method: 'PATCH',
  })
  return handleResponse(res)
}

export async function rescheduleTask(taskId, data) {
  const res = await authFetch(`${BASE}/tasks/${taskId}/reschedule`, {
    method: 'PATCH',
    body: JSON.stringify(data),
  })
  return handleResponse(res)
}
