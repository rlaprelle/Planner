import { authFetch, handleResponse } from './client'

const BASE = '/api/v1'

export async function getProjectEvents(projectId) {
  const res = await authFetch(`${BASE}/projects/${projectId}/events`)
  return handleResponse(res)
}

export async function createEvent(projectId, data) {
  const res = await authFetch(`${BASE}/projects/${projectId}/events`, {
    method: 'POST',
    body: JSON.stringify(data),
  })
  return handleResponse(res)
}

export async function getEventsForDate(date) {
  const res = await authFetch(`${BASE}/events/for-date?date=${date}`)
  return handleResponse(res)
}

export async function getEvent(eventId) {
  const res = await authFetch(`${BASE}/events/${eventId}`)
  return handleResponse(res)
}

export async function updateEvent(eventId, data) {
  const res = await authFetch(`${BASE}/events/${eventId}`, {
    method: 'PUT',
    body: JSON.stringify(data),
  })
  return handleResponse(res)
}

export async function archiveEvent(eventId) {
  const res = await authFetch(`${BASE}/events/${eventId}/archive`, {
    method: 'PATCH',
  })
  return handleResponse(res)
}
