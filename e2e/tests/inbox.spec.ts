import { test, expect } from '../fixtures/auth'

const DEFERRED_ITEMS = [
  {
    id: 'deferred-1',
    userId: 'u1',
    rawText: 'Buy more coffee beans',
    capturedAt: '2026-04-01T10:00:00Z',
    status: 'PENDING',
    deferUntil: null,
    createdAt: '2026-04-01T10:00:00Z',
    updatedAt: '2026-04-01T10:00:00Z',
  },
  {
    id: 'deferred-2',
    userId: 'u1',
    rawText: 'Schedule dentist appointment',
    capturedAt: '2026-04-01T11:30:00Z',
    status: 'PENDING',
    deferUntil: null,
    createdAt: '2026-04-01T11:30:00Z',
    updatedAt: '2026-04-01T11:30:00Z',
  },
]

test.describe('Inbox', () => {
  test('shows pending deferred items', async ({ page }) => {
    // Override the auth fixture's deferred mock before navigating
    await page.route('**/api/v1/deferred', route =>
      route.fulfill({ json: DEFERRED_ITEMS })
    )

    await page.goto('/inbox')

    await expect(page.getByRole('heading', { name: 'Inbox' })).toBeVisible()

    // Both item texts should be visible
    await expect(page.getByText('Buy more coffee beans')).toBeVisible()
    await expect(page.getByText('Schedule dentist appointment')).toBeVisible()

    // Each item should have Convert, Defer, and Dismiss action buttons
    const convertButtons = page.getByRole('button', { name: 'Convert' })
    await expect(convertButtons).toHaveCount(2)

    const deferButtons = page.getByRole('button', { name: 'Defer' })
    await expect(deferButtons).toHaveCount(2)

    const dismissButtons = page.getByRole('button', { name: 'Dismiss' })
    await expect(dismissButtons).toHaveCount(2)
  })

  test('shows empty state when no items', async ({ page }) => {
    // Auth fixture already mocks /api/v1/deferred with [] — no override needed
    await page.goto('/inbox')

    await expect(page.getByRole('heading', { name: 'Inbox' })).toBeVisible()
    await expect(page.getByText('Nothing to process.')).toBeVisible()
  })
})
