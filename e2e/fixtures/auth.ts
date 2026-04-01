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
    await use(page)
  },
})

export { expect } from '@playwright/test'
