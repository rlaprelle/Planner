import { authFetch } from './client'

const BASE = '/api/v1/projects'

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
  // 204 No Content
  if (res.status === 204) return null
  return res.json()
}

export async function getProjects() {
  const res = await authFetch(BASE)
  return handleResponse(res)
}

export async function getProject(id) {
  const res = await authFetch(`${BASE}/${id}`)
  return handleResponse(res)
}

export async function createProject(data) {
  const res = await authFetch(BASE, {
    method: 'POST',
    body: JSON.stringify(data),
  })
  return handleResponse(res)
}

export async function updateProject(id, data) {
  const res = await authFetch(`${BASE}/${id}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  })
  return handleResponse(res)
}

export async function archiveProject(id) {
  const res = await authFetch(`${BASE}/${id}/archive`, {
    method: 'PATCH',
  })
  return handleResponse(res)
}
