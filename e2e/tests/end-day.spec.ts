import { test, expect } from '../fixtures/auth'

const COMPLETED_TASKS = [
  { id: 'task-1', title: 'Write tests', status: 'COMPLETED' },
  { id: 'task-2', title: 'Review PR', status: 'COMPLETED' },
]

async function mockEndDayDeps(page: any) {
  // Triage phase: no active tasks → auto-skips
  await page.route('**/api/v1/tasks/active', route =>
    route.fulfill({ json: [] })
  )
  // Stats streak for completion screen
  await page.route('**/api/v1/stats/streak', route =>
    route.fulfill({ json: { streak: 3 } })
  )
  // Reflection save endpoint
  await page.route('**/api/v1/schedule/today/reflect', route =>
    route.fulfill({ json: {} })
  )
}

test.describe('End of Day', () => {
  test('auto-advances through empty triage and inbox to reflection', async ({ page }) => {
    await mockEndDayDeps(page)
    // Auth fixture mocks /deferred as [] — inbox auto-skips too
    await page.route('**/api/v1/tasks/completed-today', route =>
      route.fulfill({ json: [] })
    )

    await page.goto('/end-day')

    // With no active tasks and no deferred items, triage and inbox auto-skip
    // and we land on the reflection phase
    await expect(page.getByRole('heading', { name: 'End of Day' })).toBeVisible()
    await expect(page.getByText('How did today go?')).toBeVisible()
  })

  test('shows current deferred item with action buttons when inbox has items', async ({ page }) => {
    await mockEndDayDeps(page)
    await page.route('**/api/v1/tasks/completed-today', route =>
      route.fulfill({ json: [] })
    )
    await page.route('**/api/v1/deferred', route =>
      route.fulfill({
        json: [
          {
            id: 'deferred-1',
            userId: 'u1',
            rawText: 'Call the plumber',
            capturedAt: '2026-04-01T09:00:00Z',
            status: 'PENDING',
            deferUntil: null,
            createdAt: '2026-04-01T09:00:00Z',
            updatedAt: '2026-04-01T09:00:00Z',
          },
        ],
      })
    )

    await page.goto('/end-day')

    await expect(page.getByRole('heading', { name: 'End of Day' })).toBeVisible()
    // Triage auto-skips (no active tasks), lands on inbox with 1 item
    await expect(page.getByText('1 of 1')).toBeVisible()
    await expect(page.getByText('Call the plumber')).toBeVisible()

    // Action buttons rendered by DeferredItemActions
    await expect(page.getByRole('button', { name: 'Task' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Event' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Defer' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Dismiss' })).toBeVisible()
  })

  test('shows reflection form with energy and mood inputs', async ({ page }) => {
    await mockEndDayDeps(page)
    await page.route('**/api/v1/tasks/completed-today', route =>
      route.fulfill({ json: [] })
    )

    await page.goto('/end-day')

    // Auto-advances to reflection (empty triage + empty inbox)
    await expect(page.getByText('How did today go?')).toBeVisible()

    // Energy range input
    await expect(page.getByText(/Energy/)).toBeVisible()
    // Mood range input
    await expect(page.getByText(/Mood/)).toBeVisible()

    // Notes textarea
    await expect(page.getByPlaceholder('Anything on your mind?')).toBeVisible()

    // Submit button
    await expect(page.getByRole('button', { name: 'Continue' })).toBeVisible()
  })

  test('shows completed tasks in reflection form when tasks exist', async ({ page }) => {
    await mockEndDayDeps(page)
    await page.route('**/api/v1/tasks/completed-today', route =>
      route.fulfill({ json: COMPLETED_TASKS })
    )

    await page.goto('/end-day')

    // Auto-advances to reflection
    await expect(page.getByText('Completed today')).toBeVisible()
    await expect(page.getByText('Write tests')).toBeVisible()
    await expect(page.getByText('Review PR')).toBeVisible()
  })

  test('submits reflection and shows streak confirmation', async ({ page }) => {
    await mockEndDayDeps(page)
    await page.route('**/api/v1/tasks/completed-today', route =>
      route.fulfill({ json: [] })
    )

    await page.goto('/end-day')

    // Auto-advances to reflection
    await expect(page.getByRole('button', { name: 'Continue' })).toBeVisible()
    await page.getByRole('button', { name: 'Continue' }).click()

    // After successful submission: streak message for streak=3
    await expect(page.getByText('3 days in a row. Keep it going.')).toBeVisible()
    await expect(page.getByText("That's a wrap for today.")).toBeVisible()
    await expect(page.getByRole('button', { name: 'Done' })).toBeVisible()
  })
})
