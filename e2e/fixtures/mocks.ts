import type { Page } from '@playwright/test'
import { TASKS, BLOCKS, DASHBOARD, SESSION_BLOCK, SESSION_TASK_DETAIL } from './data'

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

export async function mockSessionBlock(page: Page, block = SESSION_BLOCK) {
  await page.route(/\/api\/v1\/schedule\/today(?!\/)/, (route) =>
    route.fulfill({ json: [block] })
  )
}

export async function mockTaskDetail(page: Page, task = SESSION_TASK_DETAIL) {
  await page.route(/\/api\/v1\/tasks\/[^/]+$/, (route) =>
    route.fulfill({ json: task })
  )
}

export async function mockSessionEndpoints(page: Page) {
  await page.route(/\/api\/v1\/time-blocks\/[^/]+\/start/, (route) =>
    route.fulfill({
      json: { ...SESSION_BLOCK, actualStart: new Date().toISOString() },
    })
  )
  await page.route(/\/api\/v1\/time-blocks\/[^/]+\/complete/, (route) =>
    route.fulfill({
      json: {
        ...SESSION_BLOCK,
        actualStart: new Date().toISOString(),
        actualEnd: new Date().toISOString(),
        wasCompleted: true,
        task: { ...SESSION_BLOCK.task, status: 'DONE' },
      },
    })
  )
  await page.route(/\/api\/v1\/time-blocks\/[^/]+\/done-for-now/, (route) =>
    route.fulfill({
      json: {
        ...SESSION_BLOCK,
        actualStart: new Date().toISOString(),
        actualEnd: new Date().toISOString(),
        wasCompleted: false,
      },
    })
  )
  await page.route(/\/api\/v1\/time-blocks\/[^/]+\/extend/, (route) =>
    route.fulfill({
      json: {
        ...SESSION_BLOCK,
        id: 'block-extended-1',
        startTime: '09:45:00',
        endTime: '10:15:00',
        sortOrder: 1,
      },
    })
  )
}
