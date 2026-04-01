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

export async function startTimeBlock(blockId) {
  const res = await authFetch(`${BASE}/time-blocks/${blockId}/start`, {
    method: 'PATCH',
  })
  return handleResponse(res)
}

export async function completeTimeBlock(blockId) {
  const res = await authFetch(`${BASE}/time-blocks/${blockId}/complete`, {
    method: 'PATCH',
  })
  return handleResponse(res)
}

export async function doneForNowTimeBlock(blockId) {
  const res = await authFetch(`${BASE}/time-blocks/${blockId}/done-for-now`, {
    method: 'PATCH',
  })
  return handleResponse(res)
}

export async function extendTimeBlock(blockId, durationMinutes) {
  const res = await authFetch(`${BASE}/time-blocks/${blockId}/extend`, {
    method: 'POST',
    body: JSON.stringify({ durationMinutes }),
  })
  return handleResponse(res)
}
