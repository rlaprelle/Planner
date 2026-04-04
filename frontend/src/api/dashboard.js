import { authFetch, handleResponse } from './client'

export async function getDashboard() {
  const res = await authFetch('/api/v1/stats/dashboard')
  return handleResponse(res)
}

export async function getWeeklySummary() {
  const res = await authFetch('/api/v1/stats/weekly-summary')
  return handleResponse(res)
}
