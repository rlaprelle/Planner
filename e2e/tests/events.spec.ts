import { test, expect } from '../fixtures/auth'
import {
  mockSuggestedTasks,
  mockScheduleTodayWithEvents,
  mockScheduleToday,
  mockProjects,
  mockEventsForDate,
  EVENT_BLOCKS,
} from '../fixtures/mocks'

const DEFERRED_ITEMS = [
  {
    id: 'deferred-evt-1',
    userId: 'u1',
    rawText: 'Team sync meeting',
    capturedAt: '2026-04-04T08:00:00Z',
    status: 'PENDING',
    deferUntil: null,
    createdAt: '2026-04-04T08:00:00Z',
    updatedAt: '2026-04-04T08:00:00Z',
  },
]

test.describe('Events', () => {
  test('event blocks render on schedule grid', async ({ page }) => {
    await mockSuggestedTasks(page, [])
    await mockScheduleTodayWithEvents(page)
    await mockEventsForDate(page)
    await page.goto('/start-day')

    // The event label may be clipped by overflow:hidden in narrow viewports,
    // so check DOM presence rather than visual visibility
    await expect(page.getByText('Daily Standup')).toBeAttached()
  })

  test('deferred items show Task and Event buttons', async ({ page }) => {
    await page.route('**/api/v1/deferred', route =>
      route.fulfill({ json: DEFERRED_ITEMS })
    )
    await page.goto('/inbox')

    await expect(page.getByRole('button', { name: 'Task' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Event' })).toBeVisible()
  })

  test('convert-to-event form renders and has submit button', async ({ page }) => {
    await page.route('**/api/v1/deferred', route =>
      route.fulfill({ json: DEFERRED_ITEMS })
    )
    await mockProjects(page)
    await page.goto('/inbox')

    await page.getByRole('button', { name: 'Event' }).click()

    // Form fields should be visible (labels are plain text, not <label for=...>)
    await expect(page.getByText('Date *')).toBeVisible()
    await expect(page.getByText('Start *')).toBeVisible()
    await expect(page.getByText('End *')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Create event' })).toBeVisible()
  })

  test('event blocks have no resize handle', async ({ page }) => {
    // Schedule with both an event block and a task block (non-overlapping times)
    const mixedBlocks = [
      ...EVENT_BLOCKS,
      {
        id: 'tb-task-1',
        blockDate: '2026-04-04',
        startTime: '11:00:00',
        endTime: '14:00:00',
        sortOrder: 1,
        actualStart: null,
        actualEnd: null,
        wasCompleted: false,
        task: { id: 'task-1', title: 'Write tests', status: 'TODO', pointsEstimate: 2 },
        event: null,
      },
    ]

    await mockSuggestedTasks(page, [])
    await mockScheduleToday(page, mixedBlocks)
    await mockEventsForDate(page)
    await page.goto('/start-day')

    // Both blocks should render (text may be clipped in narrow viewports)
    await expect(page.getByText('Daily Standup')).toBeAttached()
    await expect(page.getByText('Write tests')).toBeAttached()

    // The event block (amber bg) should NOT have a resize handle (cursor-ew-resize div)
    const eventBlock = page.locator('.bg-amber-100')
    await expect(eventBlock).toBeAttached()
    await expect(eventBlock.locator('.cursor-ew-resize')).toHaveCount(0)

    // The task block (the draggable div containing the task title) should HAVE a resize handle
    const taskBlock = page.locator('[aria-roledescription="draggable"]').filter({ hasText: 'Write tests' })
    await expect(taskBlock).toBeVisible()
    await expect(taskBlock.locator('.cursor-ew-resize')).toHaveCount(1)
  })
})
