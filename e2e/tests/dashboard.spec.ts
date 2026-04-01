import { test, expect } from '../fixtures/auth'
import { mockDashboard } from '../fixtures/mocks'

test.describe('Dashboard', () => {
  test('cards show mocked data', async ({ page }) => {
    await mockDashboard(page)
    await page.goto('/')

    // Today at a Glance card: "1 / 3" progress
    await expect(page.getByText('1 / 3')).toBeVisible()

    // Progress bar fill should have non-zero width (33%)
    const progressFill = page.locator('[data-testid="progress-fill"]')
    const width = await progressFill.evaluate(el => (el as HTMLElement).style.width)
    expect(width).not.toBe('0%')
    expect(width).not.toBe('')

    // Planning Streak card: "5 days in a row"
    await expect(page.getByText('days in a row')).toBeVisible()
  })

  test('shows no-plan state when no blocks are scheduled', async ({ page }) => {
    await mockDashboard(page, { todayBlockCount: 0 })
    await page.goto('/')

    await expect(page.getByText('No plan yet.')).toBeVisible()
    await expect(page.getByRole('link', { name: 'Start planning →' })).toBeVisible()
  })

  test('success toast appears then auto-dismisses', async ({ page }) => {
    await mockDashboard(page)
    await page.goto('/')

    // Inject successMessage into location state using React Router v6's history format
    // (RRv6 stores user state under the `usr` key), then reload so React reads it on mount
    await page.evaluate(() =>
      history.replaceState({ usr: { successMessage: 'Plan saved. Good luck today!' }, key: 'testkey' }, '')
    )
    await page.reload()

    const toast = page.getByText('Plan saved. Good luck today!')
    await expect(toast).toBeVisible()

    // Toast auto-dismisses after 3.5 s — wait up to 5 s for it to disappear
    await expect(toast).not.toBeVisible({ timeout: 5000 })
  })
})
