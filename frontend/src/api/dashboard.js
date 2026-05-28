import { authFetch, handleResponse } from './client'
import { API_BASE } from './config'

const BASE = `${API_BASE}/api/v1/stats`

export async function getDashboard() {
  const res = await authFetch(`${BASE}/dashboard`)
  return handleResponse(res)
}

export async function getWeeklySummary() {
  const res = await authFetch(`${BASE}/weekly-summary`)
  return handleResponse(res)
}
