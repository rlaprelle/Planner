import { authFetch, handleResponse } from './client'
import { API_BASE } from './config'

const BASE = `${API_BASE}/api/v1`

export async function saveReflection(payload) {
  const res = await authFetch(`${BASE}/schedule/today/reflect`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
  return handleResponse(res)
}

export async function getStreak() {
  const res = await authFetch(`${BASE}/stats/streak`)
  return handleResponse(res)
}
