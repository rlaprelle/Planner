import { authFetch, handleResponse } from './client'

const BASE = '/api/v1'

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
