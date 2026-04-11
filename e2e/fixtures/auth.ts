import { test as base } from '@playwright/test'

export const test = base.extend({
  page: async ({ page }, use) => {
    // AuthProvider calls this on mount to restore session
    await page.route('**/api/v1/auth/refresh', route =>
      route.fulfill({
        json: {
          accessToken: 'test-token',
          user: { id: 'u1', displayName: 'Test User', timezone: 'America/New_York' },
        },
      })
    )
    // AppLayout sidebar: fetches deferred items for inbox badge count
    await page.route('**/api/v1/deferred', route =>
      route.fulfill({ json: [] })
    )
    // User preferences: fetched by settings page and ritual components
    await page.route('**/api/v1/user/preferences', route =>
      route.fulfill({
        json: {
          displayName: 'Test User',
          timezone: 'America/New_York',
          defaultStartTime: '08:00:00',
          defaultEndTime: '17:00:00',
          defaultSessionMinutes: 60,
          weekStartDay: 'MONDAY',
          ceremonyDay: 'FRIDAY',
        },
      })
    )
    await use(page)
  },
})

export { expect } from '@playwright/test'
