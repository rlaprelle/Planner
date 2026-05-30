import { test, expect } from '../fixtures/auth'

const PREFERENCES = {
  email: 'test@example.com',
  displayName: 'Test User',
  timezone: 'America/New_York',
  defaultStartTime: '09:00:00',
  defaultEndTime: '17:00:00',
  defaultSessionMinutes: 60,
  weekStartDay: 'MONDAY',
  ceremonyDay: 'FRIDAY',
}

async function mockPreferences(page: any) {
  await page.route('**/api/v1/user/preferences', route => {
    if (route.request().method() === 'GET') {
      route.fulfill({ json: PREFERENCES })
    } else if (route.request().method() === 'PATCH') {
      const body = route.request().postDataJSON()
      route.fulfill({ json: { ...PREFERENCES, ...body } })
    }
  })
}

test.describe('Settings page', () => {
  test('loads and displays current preferences', async ({ page }) => {
    await mockPreferences(page)
    await page.goto('/settings')

    await expect(page.getByRole('heading', { name: 'Settings' })).toBeVisible()
    await expect(page.locator('#displayName')).toHaveValue('Test User')
    await expect(page.locator('#weekStartDay')).toHaveValue('MONDAY')
    await expect(page.locator('#ceremonyDay')).toHaveValue('FRIDAY')
  })

  test('saving changes shows success message', async ({ page }) => {
    await mockPreferences(page)
    await page.goto('/settings')

    await page.locator('#ceremonyDay').selectOption('THURSDAY')
    await page.getByRole('button', { name: 'Save' }).click()

    await expect(page.getByText('Preferences saved!')).toBeVisible()
  })

  test('settings link visible in sidebar', async ({ page }) => {
    await mockPreferences(page)

    // Mock dashboard endpoints so homepage loads
    await page.route('**/api/v1/stats/**', route =>
      route.fulfill({ json: {} })
    )
    await page.route('**/api/v1/schedule/today', route =>
      route.fulfill({ json: [] })
    )
    await page.route('**/api/v1/tasks/**', route =>
      route.fulfill({ json: [] })
    )
    await page.route('**/api/v1/events/**', route =>
      route.fulfill({ json: [] })
    )

    await page.goto('/')

    const settingsLink = page.getByRole('link', { name: 'Settings' })
    await expect(settingsLink).toBeVisible()
  })

  test('backend validation error displays on page', async ({ page }) => {
    await page.route('**/api/v1/user/preferences', route => {
      if (route.request().method() === 'GET') {
        route.fulfill({ json: PREFERENCES })
      } else if (route.request().method() === 'PATCH') {
        route.fulfill({
          status: 422,
          json: { detail: 'Start time must be before end time' },
        })
      }
    })

    await page.goto('/settings')
    await page.locator('#ceremonyDay').selectOption('THURSDAY')
    await page.getByRole('button', { name: 'Save' }).click()

    await expect(page.getByRole('alert')).toBeVisible()
  })
})

test.describe('Account section', () => {
  test('shows the current email', async ({ page }) => {
    await mockPreferences(page)
    await page.goto('/settings')

    await expect(page.getByRole('heading', { name: 'Account' })).toBeVisible()
    await expect(page.getByText('test@example.com')).toBeVisible()
  })

  test('changing password shows success and updates the access token', async ({ page }) => {
    await mockPreferences(page)
    await page.route('**/api/v1/user/password', route =>
      route.fulfill({ json: { accessToken: 'rotated-token' } })
    )
    await page.goto('/settings')

    await page.locator('#currentPassword').fill('secret123')
    await page.locator('#newPassword').fill('newsecret123')
    await page.locator('#confirmNewPassword').fill('newsecret123')
    await page.getByRole('button', { name: 'Update password' }).click()

    await expect(page.getByText('Password updated!')).toBeVisible()
  })

  test('mismatched new passwords are caught client-side', async ({ page }) => {
    await mockPreferences(page)
    let called = false
    await page.route('**/api/v1/user/password', route => {
      called = true
      route.fulfill({ json: { accessToken: 'rotated-token' } })
    })
    await page.goto('/settings')

    await page.locator('#currentPassword').fill('secret123')
    await page.locator('#newPassword').fill('newsecret123')
    await page.locator('#confirmNewPassword').fill('different456')
    await page.getByRole('button', { name: 'Update password' }).click()

    await expect(page.getByRole('alert')).toBeVisible()
    expect(called).toBe(false)
  })

  test('wrong current password surfaces the backend error', async ({ page }) => {
    await mockPreferences(page)
    await page.route('**/api/v1/user/password', route =>
      route.fulfill({ status: 401, json: { detail: 'Current password is incorrect' } })
    )
    await page.goto('/settings')

    await page.locator('#currentPassword').fill('wrong')
    await page.locator('#newPassword').fill('newsecret123')
    await page.locator('#confirmNewPassword').fill('newsecret123')
    await page.getByRole('button', { name: 'Update password' }).click()

    await expect(page.getByText('Current password is incorrect')).toBeVisible()
  })

  test('requesting an email change shows the check-your-inbox message', async ({ page }) => {
    await mockPreferences(page)
    await page.route('**/api/v1/user/email', route =>
      route.fulfill({ status: 202, body: '' })
    )
    await page.goto('/settings')

    await page.locator('#newEmail').fill('new@example.com')
    await page.locator('#emailCurrentPassword').fill('secret123')
    await page.getByRole('button', { name: 'Send verification email' }).click()

    await expect(page.getByText(/check new@example\.com/i)).toBeVisible()
  })
})
