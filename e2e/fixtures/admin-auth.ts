import { test as base } from '@playwright/test'
import { ADMIN_JWT } from './admin-data'

/**
 * Admin equivalent of `./auth`. AdminRoute redirects unauthenticated or
 * non-admin users away from /admin/*, so admin tests must mock /auth/refresh
 * with a JWT whose payload includes `role: ADMIN`. The frontend decodes the
 * payload without verifying the signature.
 */
export const test = base.extend({
  page: async ({ page }, use) => {
    await page.route('**/api/v1/auth/refresh', route =>
      route.fulfill({
        json: {
          accessToken: ADMIN_JWT,
          user: { id: 'u1', email: 'admin@test.local', displayName: 'Admin', timezone: 'America/New_York', role: 'ADMIN' },
        },
      })
    )
    await page.route('**/api/v1/deferred', route =>
      route.fulfill({ json: [] })
    )
    await page.route('**/api/v1/user/preferences', route =>
      route.fulfill({
        json: {
          displayName: 'Admin',
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
