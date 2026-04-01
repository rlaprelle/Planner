import type { Page } from '@playwright/test'
import { TASKS, BLOCKS, DASHBOARD } from './data'

export async function mockDashboard(
  page: Page,
  override: Partial<typeof DASHBOARD> = {}
) {
  await page.route('**/api/v1/stats/dashboard', route =>
    route.fulfill({ json: { ...DASHBOARD, ...override } })
  )
}

export async function mockSuggestedTasks(page: Page, tasks = TASKS) {
  await page.route(/\/api\/v1\/tasks\/suggested/, route =>
    route.fulfill({ json: tasks })
  )
}

export async function mockScheduleToday(page: Page, blocks = BLOCKS) {
  await page.route(/\/api\/v1\/schedule\/today(?!\/)/, route =>
    route.fulfill({ json: blocks })
  )
}

export async function mockSavePlan(page: Page) {
  await page.route('**/api/v1/schedule/today/plan', route =>
    route.fulfill({ json: [] })
  )
}
