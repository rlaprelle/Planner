import { test, expect } from '../fixtures/auth'
import { mockDashboard, mockProjects, mockProjectDetail } from '../fixtures/mocks'

// Phone-sized viewport for all tests in this file
test.use({ viewport: { width: 375, height: 667 }, hasTouch: true })

test.describe('Mobile layout', () => {
  test('sidebar is an off-canvas drawer behind a hamburger button', async ({ page }) => {
    await mockDashboard(page)
    await mockProjects(page)
    await page.goto('/')

    const nav = page.getByRole('navigation', { name: 'Main navigation' })

    // Drawer starts off-canvas; hamburger is visible
    await expect(nav).not.toBeInViewport()
    const hamburger = page.getByRole('button', { name: 'Open navigation menu' })
    await expect(hamburger).toBeVisible()

    // Opening the drawer reveals the nav links
    await hamburger.click()
    await expect(nav).toBeInViewport()
    await expect(nav.getByRole('link', { name: 'Projects' })).toBeVisible()

    // Close button slides the drawer back out
    await nav.getByRole('button', { name: 'Close navigation menu' }).click()
    await expect(nav).not.toBeInViewport()
  })

  test('navigating from the drawer closes it', async ({ page }) => {
    await mockDashboard(page)
    await mockProjects(page)
    await page.goto('/')

    await page.getByRole('button', { name: 'Open navigation menu' }).click()
    const nav = page.getByRole('navigation', { name: 'Main navigation' })
    await nav.getByRole('link', { name: 'Projects' }).click()

    await expect(page).toHaveURL(/\/projects$/)
    await expect(nav).not.toBeInViewport()
  })

  test('dashboard has no horizontal scroll at 375px', async ({ page }) => {
    await mockDashboard(page)
    await page.goto('/')
    await expect(page.getByText('days in a row')).toBeVisible()

    // <main> is the app's scroll container (its overflow-y: auto forces
    // overflow-x to compute to auto), so horizontal overflow shows up
    // there — the document itself never grows.
    const overflow = await page.evaluate(() => {
      const main = document.querySelector('main')!
      return main.scrollWidth - main.clientWidth
    })
    expect(overflow).toBe(0)
  })

  test('task details open as a dialog that fits the viewport', async ({ page }) => {
    await mockProjects(page)
    await mockProjectDetail(page)
    await page.goto('/projects')

    await page.getByRole('button', { name: 'Write tests' }).click()

    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible()
    await expect(dialog.getByLabel('Task title')).toHaveValue('Write tests')

    // The dialog must not overflow the phone viewport
    const box = await dialog.boundingBox()
    expect(box!.x).toBeGreaterThanOrEqual(0)
    expect(box!.x + box!.width).toBeLessThanOrEqual(375)
  })
})
