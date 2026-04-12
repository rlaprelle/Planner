import { test, expect } from '../fixtures/auth'
import { mockDashboard, mockScheduleToday, mockEventsForDate } from '../fixtures/mocks'

// Helper: open QuickCapture via Ctrl+Space and wait for the dialog
async function openQuickCapture(page) {
  // Click the page body first to ensure focus is on the page
  await page.locator('body').click()
  await page.keyboard.press('Control+Space')
  await expect(page.getByRole('dialog')).toBeAttached()
}

// Helper: mock the POST endpoint and track calls
function mockDeferredPost(page) {
  const calls: string[] = []
  page.route('**/api/v1/deferred', (route) => {
    if (route.request().method() === 'POST') {
      const body = route.request().postDataJSON()
      calls.push(body.rawText)
      return route.fulfill({ json: { id: `d-${calls.length}`, rawText: body.rawText, status: 'PENDING' } })
    }
    // GET requests fall through to the auth fixture mock (returns [])
    return route.fulfill({ json: [] })
  })
  return calls
}

test.describe('QuickCapture', () => {
  test.beforeEach(async ({ page }) => {
    await mockDashboard(page)
    await mockScheduleToday(page, [])
    await mockEventsForDate(page)
    // Mock weekly summary endpoint
    await page.route('**/api/v1/stats/weekly-summary*', route =>
      route.fulfill({ json: { completedTasks: 0, totalPoints: 0, daysPlanned: 0 } })
    )
    await page.goto('/dashboard')
  })

  test.describe('capture and close (existing behavior)', () => {
    test('Enter submits and closes modal', async ({ page }) => {
      const calls = mockDeferredPost(page)

      await openQuickCapture(page)
      await page.getByPlaceholder('What\'s on your mind?').fill('Buy groceries')
      await page.keyboard.press('Enter')

      // Should show confirmation then close
      await expect(page.getByText('Captured.')).toBeAttached()
      await expect(page.getByRole('dialog')).not.toBeAttached({ timeout: 3000 })
      expect(calls).toEqual(['Buy groceries'])
    })

    test('Capture button submits and closes modal', async ({ page }) => {
      const calls = mockDeferredPost(page)

      await openQuickCapture(page)
      await page.getByPlaceholder('What\'s on your mind?').fill('Call dentist')
      await page.getByRole('button', { name: 'Capture', exact: true }).click()

      await expect(page.getByText('Captured.')).toBeAttached()
      await expect(page.getByRole('dialog')).not.toBeAttached({ timeout: 3000 })
      expect(calls).toEqual(['Call dentist'])
    })
  })

  test.describe('brain-dump mode (capture and continue)', () => {
    test('Ctrl+Enter submits and keeps modal open', async ({ page }) => {
      const calls = mockDeferredPost(page)

      await openQuickCapture(page)
      const textarea = page.getByPlaceholder('What\'s on your mind?')
      await textarea.fill('First thought')
      await page.keyboard.press('Control+Enter')

      // Modal should stay open
      await expect(page.getByRole('dialog')).toBeAttached()

      // Textarea should be cleared after flash
      await expect(textarea).toHaveValue('', { timeout: 2000 })

      // API was called
      expect(calls).toEqual(['First thought'])
    })

    test('Keep capturing button submits and keeps modal open', async ({ page }) => {
      const calls = mockDeferredPost(page)

      await openQuickCapture(page)
      await page.getByPlaceholder('What\'s on your mind?').fill('Another thought')
      await page.getByRole('button', { name: 'Keep capturing' }).click()

      await expect(page.getByRole('dialog')).toBeAttached()
      await expect(page.getByPlaceholder('What\'s on your mind?')).toHaveValue('', { timeout: 2000 })
      expect(calls).toEqual(['Another thought'])
    })

    test('multiple rapid captures in sequence', async ({ page }) => {
      const calls = mockDeferredPost(page)

      await openQuickCapture(page)
      const textarea = page.getByPlaceholder('What\'s on your mind?')

      // Capture 1
      await textarea.fill('Idea one')
      await page.keyboard.press('Control+Enter')
      await expect(textarea).toHaveValue('', { timeout: 2000 })

      // Capture 2
      await textarea.fill('Idea two')
      await page.keyboard.press('Control+Enter')
      await expect(textarea).toHaveValue('', { timeout: 2000 })

      // Capture 3
      await textarea.fill('Idea three')
      await page.keyboard.press('Control+Enter')
      await expect(textarea).toHaveValue('', { timeout: 2000 })

      // All three should have been sent
      expect(calls).toEqual(['Idea one', 'Idea two', 'Idea three'])

      // Modal should still be open
      await expect(page.getByRole('dialog')).toBeAttached()
    })
  })
})
