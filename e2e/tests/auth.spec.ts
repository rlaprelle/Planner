import { test, expect } from '@playwright/test'
import { mockDashboard } from '../fixtures/mocks'

// Shared mock response for a successful login/refresh
const AUTH_RESPONSE = {
  accessToken: 'test-token',
  user: { id: 'u1', displayName: 'Test User', timezone: 'America/New_York' },
}

// Every test must mock /auth/refresh (AuthProvider calls it on mount) and
// /deferred (AppLayout sidebar fetches it for the inbox badge count).
async function mockRefreshUnauthorized(page: Parameters<typeof mockDashboard>[0]) {
  await page.route('**/api/v1/auth/refresh', route =>
    route.fulfill({ status: 401, json: { message: 'Unauthorized' } })
  )
}

async function mockDeferred(page: Parameters<typeof mockDashboard>[0]) {
  await page.route('**/api/v1/deferred', route =>
    route.fulfill({ json: [] })
  )
}

test.describe('Login page', () => {
  test('shows login form', async ({ page }) => {
    await mockRefreshUnauthorized(page)

    await page.goto('/login')

    await expect(page.getByRole('heading', { name: 'Welcome back' })).toBeVisible()
    await expect(page.locator('#email')).toBeVisible()
    await expect(page.locator('#password')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Log in' })).toBeVisible()
  })

  test('successful login redirects to dashboard', async ({ page }) => {
    await mockRefreshUnauthorized(page)

    // Return a valid token when credentials are submitted
    await page.route('**/api/v1/auth/login', route =>
      route.fulfill({ json: AUTH_RESPONSE })
    )

    // After redirect, AuthProvider calls /auth/refresh again — now return the token
    let refreshCallCount = 0
    await page.route('**/api/v1/auth/refresh', route => {
      refreshCallCount++
      if (refreshCallCount === 1) {
        // First call: unauthenticated page load → 401
        route.fulfill({ status: 401, json: { message: 'Unauthorized' } })
      } else {
        // Subsequent calls: session is active after login
        route.fulfill({ json: AUTH_RESPONSE })
      }
    })

    await mockDeferred(page)
    await mockDashboard(page)

    await page.goto('/login')

    await page.locator('#email').fill('user@example.com')
    await page.locator('#password').fill('password123')
    await page.getByRole('button', { name: 'Log in' }).click()

    await expect(page).toHaveURL('/', { timeout: 5000 })
  })

  test('shows error on invalid credentials', async ({ page }) => {
    await mockRefreshUnauthorized(page)

    await page.route('**/api/v1/auth/login', route =>
      route.fulfill({ status: 401, json: { message: 'Invalid credentials' } })
    )

    await page.goto('/login')

    await page.locator('#email').fill('wrong@example.com')
    await page.locator('#password').fill('wrongpassword')
    await page.getByRole('button', { name: 'Log in' }).click()

    await expect(page.getByRole('alert')).toHaveText('Invalid email or password.')
  })

  test('has link to register page', async ({ page }) => {
    await mockRefreshUnauthorized(page)

    await page.goto('/login')

    const registerLink = page.getByRole('link', { name: 'Create one' })
    await expect(registerLink).toBeVisible()
    await registerLink.click()

    await expect(page).toHaveURL('/register')
  })
})

test.describe('Register page', () => {
  test('shows registration form', async ({ page }) => {
    await mockRefreshUnauthorized(page)

    await page.goto('/register')

    await expect(page.getByRole('heading', { name: 'Create your account' })).toBeVisible()
    await expect(page.locator('#displayName')).toBeVisible()
    await expect(page.locator('#email')).toBeVisible()
    await expect(page.locator('#password')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Create account' })).toBeVisible()
  })

  test('successful registration redirects to dashboard', async ({ page }) => {
    await mockRefreshUnauthorized(page)

    await page.route('**/api/v1/auth/register', route =>
      route.fulfill({ status: 201, json: {} })
    )

    // After registration, the page auto-logs in via /auth/login then navigates to /
    await page.route('**/api/v1/auth/login', route =>
      route.fulfill({ json: AUTH_RESPONSE })
    )

    await mockDeferred(page)
    await mockDashboard(page)

    await page.goto('/register')

    await page.locator('#displayName').fill('Test User')
    await page.locator('#email').fill('newuser@example.com')
    await page.locator('#password').fill('securepassword')
    await page.getByRole('button', { name: 'Create account' }).click()

    await expect(page).toHaveURL('/', { timeout: 5000 })
  })
})
