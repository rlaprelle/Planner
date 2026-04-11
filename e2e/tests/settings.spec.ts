import { test, expect } from '../fixtures/auth'

const PREFERENCES = {
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
