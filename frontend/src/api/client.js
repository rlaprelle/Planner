import { refreshToken } from './auth'

let currentToken = null

export function setAuthToken(token) {
  currentToken = token
}

export function getAuthToken() {
  return currentToken
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
      res = await doRequest(currentToken)
    } catch {
      // Refresh failed — caller will receive the 401 response
    }
  }

  return res
}
