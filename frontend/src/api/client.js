import { refreshToken } from './auth'

let currentToken = null
let onTokenRefreshed = null
let onAuthFailure = null

export function setAuthToken(token) {
  currentToken = token
}

export function getAuthToken() {
  return currentToken
}

export function setTokenRefreshedCallback(cb) {
  onTokenRefreshed = cb
}

export function setAuthFailureCallback(cb) {
  onAuthFailure = cb
}

/**
 * authFetch wraps fetch with:
 * - Authorization: Bearer <token> header injection
 * - Automatic token refresh on 401, with a single retry
 */
export async function authFetch(url, options = {}) {
  const doRequest = (token) => {
    const headers = {
      'Content-Type': 'application/json',
      ...options.headers,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    }
    return fetch(url, { credentials: 'include', ...options, headers })
  }

  let res = await doRequest(currentToken)

  if (res.status === 401) {
    try {
      const data = await refreshToken()
      currentToken = data.accessToken
      onTokenRefreshed?.(data.accessToken)
      res = await doRequest(currentToken)
    } catch {
      // Refresh failed — notify auth failure handler and return the 401 response
      onAuthFailure?.()
    }
  }

  return res
}

export async function handleResponse(res) {
  if (!res.ok) {
    let message = `HTTP ${res.status}`
    try {
      const body = await res.json()
      message = body.message || body.error || body.detail || message
    } catch {
      // ignore parse errors
    }
    const err = new Error(message)
    err.status = res.status
    throw err
  }
  // 204 No Content
  if (res.status === 204) return null
  return res.json()
}
