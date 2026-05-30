import { authFetch, handleResponse } from './client'
import { API_BASE } from './config'

const BASE = `${API_BASE}/api/v1`

/**
 * Change the current user's password. Requires the current password for
 * re-authentication. The backend rotates the session and returns a fresh
 * access token (the refresh cookie is rotated via Set-Cookie).
 */
export async function changePassword(currentPassword, newPassword) {
  const res = await authFetch(`${BASE}/user/password`, {
    method: 'POST',
    body: JSON.stringify({ currentPassword, newPassword }),
  })
  return handleResponse(res)
}

/**
 * Begin changing the current user's email. Requires the current password.
 * A verification link is emailed to the new address; the email only changes
 * once that link is followed. Resolves with null (202 No body).
 */
export async function requestEmailChange(newEmail, currentPassword) {
  const res = await authFetch(`${BASE}/user/email`, {
    method: 'POST',
    body: JSON.stringify({ newEmail, currentPassword }),
  })
  // 202 Accepted carries no body; only parse (to surface the message) on error.
  if (!res.ok) return handleResponse(res)
  return null
}

/**
 * Complete an email change using the token from a verification link. No auth
 * required — the token is the proof. On success the backend revokes all
 * sessions, so the caller should send the user back to log in.
 */
export async function confirmEmailChange(token) {
  const res = await fetch(`${BASE}/auth/email/confirm`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({ token }),
  })
  return handleResponse(res)
}
