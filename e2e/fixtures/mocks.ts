import type { Page } from '@playwright/test'
import { TASKS, BLOCKS, EVENTS, DASHBOARD, SESSION_BLOCK, SESSION_TASK_DETAIL } from './data'

export const EVENT_BLOCKS = [
  {
    id: 'tb-evt-1',
    blockDate: '2026-04-04',
    startTime: '09:00:00',
    endTime: '10:00:00',
    sortOrder: 0,
    actualStart: null,
    actualEnd: null,
    wasCompleted: false,
    task: null,
    event: {
      id: 'evt-1',
      title: 'Daily Standup',
      projectId: 'proj-1',
      projectName: 'Work',
      projectColor: '#6366f1',
      energyLevel: 'LOW',
    },
  },
]

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

export async function mockScheduleTodayWithEvents(page: Page, blocks = EVENT_BLOCKS) {
  await page.route(/\/api\/v1\/schedule\/today(?!\/)/, route =>
    route.fulfill({ json: blocks })
  )
}

export async function mockProjects(page: Page) {
  await page.route('**/api/v1/projects', route =>
    route.fulfill({
      json: [
        { id: 'proj-1', name: 'Work', color: '#6366f1', isActive: true },
      ],
    })
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

export async function mockEventsForDate(page: Page, events: unknown[] = []) {
  await page.route(/\/api\/v1\/events\/for-date/, route =>
    route.fulfill({ json: events })
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
