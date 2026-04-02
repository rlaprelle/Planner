import { test, expect } from '@playwright/test'
import { ADMIN_USERS, ADMIN_PROJECTS, ADMIN_TASKS, ADMIN_DEFERRED, ADMIN_REFLECTIONS, ADMIN_TIME_BLOCKS, USER_DEPENDENTS } from '../fixtures/admin-data'

async function mockAdminApi(page) {
  // Mock auth refresh to prevent redirect — the admin page is rendered inside the BrowserRouter
  // which also contains AuthProvider. Even though /admin routes don't require ProtectedRoute,
  // the AuthProvider still fires a refresh call on mount.
  await page.route('**/api/v1/auth/refresh', route =>
    route.fulfill({ status: 401, json: { message: 'no session' } })
  )

  await page.route('**/api/v1/admin/users', route => {
    if (route.request().method() === 'GET') return route.fulfill({ json: ADMIN_USERS })
    if (route.request().method() === 'POST') return route.fulfill({ status: 201, json: { ...ADMIN_USERS[0], id: 'u-new' } })
    return route.continue()
  })
  await page.route('**/api/v1/admin/users/*/dependents', route =>
    route.fulfill({ json: USER_DEPENDENTS })
  )
  await page.route('**/api/v1/admin/users/*', route => {
    if (route.request().method() === 'DELETE') return route.fulfill({ status: 204 })
    if (route.request().method() === 'PUT') return route.fulfill({ json: ADMIN_USERS[0] })
    return route.continue()
  })
  await page.route('**/api/v1/admin/projects', route => route.fulfill({ json: ADMIN_PROJECTS }))
  await page.route('**/api/v1/admin/projects/*', route => {
    if (route.request().method() === 'DELETE') return route.fulfill({ status: 204 })
    return route.fulfill({ json: ADMIN_PROJECTS[0] })
  })
  await page.route('**/api/v1/admin/tasks', route => route.fulfill({ json: ADMIN_TASKS }))
  await page.route('**/api/v1/admin/tasks/*', route => {
    if (route.request().method() === 'DELETE') return route.fulfill({ status: 204 })
    return route.fulfill({ json: ADMIN_TASKS[0] })
  })
  await page.route('**/api/v1/admin/deferred-items', route => route.fulfill({ json: ADMIN_DEFERRED }))
  await page.route('**/api/v1/admin/reflections', route => route.fulfill({ json: ADMIN_REFLECTIONS }))
  await page.route('**/api/v1/admin/time-blocks', route => route.fulfill({ json: ADMIN_TIME_BLOCKS }))
  // Also mock the deferred endpoint used by the app's AppLayout sidebar badge
  await page.route('**/api/v1/deferred', route => route.fulfill({ json: [] }))
}

test.describe('Admin Page', () => {
  test.beforeEach(async ({ page }) => {
    await mockAdminApi(page)
  })

  test('loads admin page and shows users table by default', async ({ page }) => {
    await page.goto('/admin')
    await expect(page).toHaveURL(/\/admin\/users/)
    await expect(page.getByRole('heading', { name: 'Users' })).toBeVisible()
    await expect(page.getByText('alice@example.com')).toBeVisible()
    await expect(page.getByText('bob@example.com')).toBeVisible()
  })

  test('sidebar navigation switches between entity tables', async ({ page }) => {
    await page.goto('/admin')
    await page.getByRole('link', { name: 'Projects' }).click()
    await expect(page.getByRole('heading', { name: 'Projects' })).toBeVisible()
    await expect(page.getByText('Work')).toBeVisible()
  })

  test('create user modal opens and submits', async ({ page }) => {
    await page.goto('/admin/users')
    await page.getByRole('button', { name: '+ Create User' }).click()
    await expect(page.getByRole('heading', { name: 'Create User' })).toBeVisible()
    // Form fields use plain label/input siblings (no for/id binding),
    // so we locate by the label text then target the adjacent input.
    const modal = page.locator('[role="dialog"]')
    await modal.locator('input[type="email"]').fill('new@example.com')
    await modal.locator('input[type="password"]').fill('password123')
    // Display Name is a plain text input — it's the third input in the form
    await modal.locator('input[type="text"]').first().fill('New User')
    await modal.getByRole('button', { name: 'Save' }).click()
  })

  test('delete user shows cascade confirmation', async ({ page }) => {
    await page.goto('/admin/users')
    const deleteButtons = page.getByRole('button', { name: 'Delete' })
    await deleteButtons.first().click()
    await expect(page.getByText('Delete user?')).toBeVisible()
    await expect(page.getByText('1 project')).toBeVisible()
    await expect(page.getByText('1 task')).toBeVisible()
  })

  test('has back to app link', async ({ page }) => {
    await page.goto('/admin')
    await expect(page.getByText('Back to app')).toBeVisible()
  })
})
