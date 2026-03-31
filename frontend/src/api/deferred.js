import { authFetch, handleResponse } from './client'

const BASE = '/api/v1'

export async function createDeferredItem(rawText) {
  const res = await authFetch(`${BASE}/deferred`, {
    method: 'POST',
    body: JSON.stringify({ rawText }),
  })
  return handleResponse(res)
}

export async function getDeferredItems() {
  const res = await authFetch(`${BASE}/deferred`)
  return handleResponse(res)
}

export async function convertDeferredItem(id, payload) {
  const res = await authFetch(`${BASE}/deferred/${id}/convert`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })
  return handleResponse(res)
}

export async function deferDeferredItem(id, deferFor) {
  const res = await authFetch(`${BASE}/deferred/${id}/defer`, {
    method: 'POST',
    body: JSON.stringify({ deferFor }),
  })
  return handleResponse(res)
}

export async function dismissDeferredItem(id) {
  const res = await authFetch(`${BASE}/deferred/${id}/dismiss`, {
    method: 'PATCH',
  })
  return handleResponse(res)
}
