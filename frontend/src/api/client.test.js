import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('./auth', () => ({ refreshToken: vi.fn() }))

import { refreshToken } from './auth'
import {
  setAuthToken,
  setTokenRefreshedCallback,
  setAuthFailureCallback,
  authFetch,
  handleResponse,
} from './client'

// ---------------------------------------------------------------------------
// handleResponse
// ---------------------------------------------------------------------------

describe('handleResponse', () => {
  it('returns parsed JSON for a 200 response', async () => {
    const res = new Response(JSON.stringify({ id: 1, name: 'Task A' }), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    })
    const result = await handleResponse(res)
    expect(result).toEqual({ id: 1, name: 'Task A' })
  })

  it('returns null for a 204 No Content response', async () => {
    const res = new Response(null, { status: 204 })
    const result = await handleResponse(res)
    expect(result).toBeNull()
  })

  it('throws with the server error message for a non-ok response', async () => {
    const res = new Response(JSON.stringify({ message: 'Not found' }), { status: 404 })
    Object.defineProperty(res, 'ok', { value: false })

    await expect(handleResponse(res)).rejects.toThrow('Not found')
  })

  it('throws with "HTTP {status}" when the body has no message field', async () => {
    const res = new Response(JSON.stringify({ detail: 'something else' }), { status: 500 })
    Object.defineProperty(res, 'ok', { value: false })

    await expect(handleResponse(res)).rejects.toThrow('HTTP 500')
  })

  it('attaches .status to the thrown error object', async () => {
    const res = new Response(JSON.stringify({ message: 'Forbidden' }), { status: 403 })
    Object.defineProperty(res, 'ok', { value: false })

    let caught
    try {
      await handleResponse(res)
    } catch (err) {
      caught = err
    }
    expect(caught).toBeDefined()
    expect(caught.status).toBe(403)
  })
})

// ---------------------------------------------------------------------------
// authFetch
// ---------------------------------------------------------------------------

describe('authFetch', () => {
  beforeEach(() => {
    // Reset module-level state between tests
    setAuthToken(null)
    setTokenRefreshedCallback(null)
    setAuthFailureCallback(null)
    vi.clearAllMocks()

    globalThis.fetch = vi.fn()
  })

  it('adds Authorization Bearer header when a token is set', async () => {
    setAuthToken('my-access-token')
    const mockResponse = new Response('{}', { status: 200 })
    globalThis.fetch.mockResolvedValue(mockResponse)

    await authFetch('/api/v1/tasks')

    expect(globalThis.fetch).toHaveBeenCalledOnce()
    const [, options] = globalThis.fetch.mock.calls[0]
    expect(options.headers.Authorization).toBe('Bearer my-access-token')
  })

  it('retries with a new token on 401', async () => {
    setAuthToken('expired-token')

    const unauthorizedResponse = new Response('{}', { status: 401 })
    const okResponse = new Response('{}', { status: 200 })

    globalThis.fetch
      .mockResolvedValueOnce(unauthorizedResponse)
      .mockResolvedValueOnce(okResponse)

    refreshToken.mockResolvedValue({ accessToken: 'new-token' })

    const result = await authFetch('/api/v1/tasks')

    expect(globalThis.fetch).toHaveBeenCalledTimes(2)
    const [, retryOptions] = globalThis.fetch.mock.calls[1]
    expect(retryOptions.headers.Authorization).toBe('Bearer new-token')
    expect(result).toBe(okResponse)
  })

  it('calls onTokenRefreshed callback after a successful refresh', async () => {
    setAuthToken('expired-token')

    const unauthorizedResponse = new Response('{}', { status: 401 })
    const okResponse = new Response('{}', { status: 200 })

    globalThis.fetch
      .mockResolvedValueOnce(unauthorizedResponse)
      .mockResolvedValueOnce(okResponse)

    refreshToken.mockResolvedValue({ accessToken: 'new-token' })

    const onRefreshed = vi.fn()
    setTokenRefreshedCallback(onRefreshed)

    await authFetch('/api/v1/tasks')

    expect(onRefreshed).toHaveBeenCalledOnce()
    expect(onRefreshed).toHaveBeenCalledWith('new-token')
  })

  it('calls onAuthFailure callback when refresh fails', async () => {
    setAuthToken('expired-token')

    const unauthorizedResponse = new Response('{}', { status: 401 })
    globalThis.fetch.mockResolvedValue(unauthorizedResponse)

    refreshToken.mockRejectedValue(new Error('Refresh failed'))

    const onFailure = vi.fn()
    setAuthFailureCallback(onFailure)

    await authFetch('/api/v1/tasks')

    expect(onFailure).toHaveBeenCalledOnce()
  })

  it('does NOT retry on non-401 errors (e.g., 403)', async () => {
    setAuthToken('valid-token')

    const forbiddenResponse = new Response('{}', { status: 403 })
    globalThis.fetch.mockResolvedValue(forbiddenResponse)

    const result = await authFetch('/api/v1/tasks')

    expect(globalThis.fetch).toHaveBeenCalledOnce()
    expect(refreshToken).not.toHaveBeenCalled()
    expect(result).toBe(forbiddenResponse)
  })
})
