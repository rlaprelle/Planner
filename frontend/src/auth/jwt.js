// Decodes a JWT payload without verifying the signature — the server is the
// source of truth for trust; this is purely so the UI can mirror the role
// claim (e.g. show an admin nav link). Never gate anything security-sensitive
// on this output; the backend enforces role on every admin request.
export function decodeJwtPayload(token) {
  if (!token) return null
  const parts = token.split('.')
  if (parts.length !== 3) return null
  try {
    const payload = parts[1]
    const padded = payload.padEnd(payload.length + (4 - (payload.length % 4)) % 4, '=')
    const json = atob(padded.replace(/-/g, '+').replace(/_/g, '/'))
    return JSON.parse(json)
  } catch {
    return null
  }
}

export function extractRole(token) {
  const payload = decodeJwtPayload(token)
  return payload?.role ?? null
}
