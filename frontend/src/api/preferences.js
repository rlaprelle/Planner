import { authFetch, handleResponse } from './client'

const BASE = '/api/v1'

export async function getPreferences() {
  const res = await authFetch(`${BASE}/user/preferences`)
  return handleResponse(res)
}

export async function updatePreferences(data) {
  const res = await authFetch(`${BASE}/user/preferences`, {
    method: 'PATCH',
    body: JSON.stringify(data),
  })
  return handleResponse(res)
}
