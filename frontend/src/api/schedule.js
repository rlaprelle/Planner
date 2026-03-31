import { authFetch, handleResponse } from './client'

const BASE = '/api/v1'

export async function getScheduleToday() {
  const res = await authFetch(`${BASE}/schedule/today`)
  return handleResponse(res)
}

export async function savePlan(blockDate, blocks) {
  const res = await authFetch(`${BASE}/schedule/today/plan`, {
    method: 'POST',
    body: JSON.stringify({ blockDate, blocks }),
  })
  return handleResponse(res)
}

export async function getSuggestedTasks(date, limit = 50) {
  const params = new URLSearchParams({ date, limit })
  const res = await authFetch(`${BASE}/tasks/suggested?${params}`)
  return handleResponse(res)
}
