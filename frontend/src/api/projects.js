import { authFetch, handleResponse } from './client'

const BASE = '/api/v1/projects'

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
