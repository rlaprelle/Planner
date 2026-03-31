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
