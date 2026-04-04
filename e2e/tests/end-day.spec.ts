import { test, expect } from '../fixtures/auth'

const COMPLETED_TASKS = [
  { id: 'task-1', title: 'Write tests', status: 'DONE' },
  { id: 'task-2', title: 'Review PR', status: 'DONE' },
]

test.describe('End of Day', () => {
  test.beforeEach(async ({ page }) => {
    await page.route('**/api/v1/tasks/completed-today', route =>
      route.fulfill({ json: [] })
    )
    // Stats streak for after reflection is submitted
    await page.route('**/api/v1/stats/streak', route =>
      route.fulfill({ json: { streak: 3 } })
    )
    // Reflection save endpoint
    await page.route('**/api/v1/schedule/today/reflect', route =>
      route.fulfill({ json: {} })
    )
  })

  test('shows End of Day heading and inbox-clear state with no deferred items', async ({ page }) => {
    // Auth fixture already mocks /api/v1/deferred with [] — no override needed
    await page.goto('/end-day')

    await expect(page.getByRole('heading', { name: 'End of Day' })).toBeVisible()
    await expect(page.getByText('Inbox is clear.')).toBeVisible()

    // "Continue to reflection" button should be visible when inbox is empty
    await expect(page.getByRole('button', { name: /Continue to reflection/i })).toBeVisible()
  })

  test('shows current deferred item with action buttons when inbox has items', async ({ page }) => {
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
    // Progress counter: "1 of 1"
    await expect(page.getByText('1 of 1')).toBeVisible()
    await expect(page.getByText('Call the plumber')).toBeVisible()

    // Action buttons rendered by DeferredItemActions
    await expect(page.getByRole('button', { name: 'Convert' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Defer' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Dismiss' })).toBeVisible()
  })

  test('shows reflection form after clicking Continue to reflection', async ({ page }) => {
    // Auth fixture already mocks /api/v1/deferred with [] — inbox is empty
    await page.goto('/end-day')

    await page.getByRole('button', { name: /Continue to reflection/i }).click()

    // Phase 2: reflection form
    await expect(page.getByText('How did today go?')).toBeVisible()

    // Energy range input
    await expect(page.getByText(/Energy/)).toBeVisible()
    // Mood range input
    await expect(page.getByText(/Mood/)).toBeVisible()

    // Notes textarea
    await expect(page.getByPlaceholder('Anything on your mind?')).toBeVisible()

    // Submit button
    await expect(page.getByRole('button', { name: 'Wrap up the day' })).toBeVisible()
  })

  test('shows completed tasks in reflection form when tasks exist', async ({ page }) => {
    await page.route('**/api/v1/tasks/completed-today', route =>
      route.fulfill({ json: COMPLETED_TASKS })
    )

    // Auth fixture mocks deferred as [] so we go straight to phase 2
    await page.goto('/end-day')
    await page.getByRole('button', { name: /Continue to reflection/i }).click()

    await expect(page.getByText('Completed today')).toBeVisible()
    await expect(page.getByText('Write tests')).toBeVisible()
    await expect(page.getByText('Review PR')).toBeVisible()
  })

  test('submits reflection and shows streak confirmation', async ({ page }) => {
    await page.goto('/end-day')
    await page.getByRole('button', { name: /Continue to reflection/i }).click()

    await expect(page.getByRole('button', { name: 'Wrap up the day' })).toBeVisible()
    await page.getByRole('button', { name: 'Wrap up the day' }).click()

    // After successful submission: streak message for streak=3
    await expect(page.getByText('3 days in a row. Keep it going.')).toBeVisible()
    await expect(page.getByText("That's a wrap for today.")).toBeVisible()
    await expect(page.getByRole('button', { name: 'Done' })).toBeVisible()
  })
})
