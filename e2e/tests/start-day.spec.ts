import { test, expect } from '../fixtures/auth'
import { mockSuggestedTasks, mockScheduleToday, mockSavePlan, mockDashboard, mockEventsForDate } from '../fixtures/mocks'
import { TASKS, BLOCKS } from '../fixtures/data'

test.describe('Start Day', () => {
  test('task browser renders project columns', async ({ page }) => {
    await mockSuggestedTasks(page, TASKS.slice(0, 2))
    await mockScheduleToday(page, [])
    await page.goto('/start-day')

    // Project name "Work" appears as column header inside the task browser
    await expect(page.getByText('Work').first()).toBeVisible()
    await expect(page.getByText('Write tests')).toBeVisible()
    await expect(page.getByText('Review PR')).toBeVisible()
  })

  test('checkbox + add to calendar puts block on grid', async ({ page }) => {
    await mockSuggestedTasks(page, TASKS.slice(0, 1))
    await mockScheduleToday(page, [])
    await page.goto('/start-day')

    // The TaskCard renders an input[type="checkbox"] alongside the task title
    // Find the checkbox that is in the same card as "Write tests"
    const checkbox = page.getByText('Write tests').locator('xpath=ancestor::div[.//input[@type="checkbox"]]').first().locator('input[type="checkbox"]')
    await checkbox.check()

    await page.getByRole('button', { name: '+ Add to calendar' }).click()

    // Task title should now appear as a TimeBlock in the "Today's Plan" section.
    // Use the TimeBlock's draggable container (role=button from dnd-kit) rather than the
    // inner text span, which is clipped by overflow:hidden and appears hidden to Playwright.
    // .first() selects the draggable container over the hover-only Remove button.
    const planSection = page.locator('section').filter({ hasText: "Today's Plan" })
    await expect(planSection.getByRole('button', { name: /Write tests/ }).first()).toBeVisible()
  })

  test('confirm plan navigates to dashboard with toast', async ({ page }) => {
    await mockSuggestedTasks(page, TASKS.slice(0, 1))
    // Pre-populate calendar with one block so "Confirm plan" is enabled
    await mockScheduleToday(page, [BLOCKS[0]])
    await mockSavePlan(page)
    // Dashboard mock needed for the page that loads after navigation
    await mockDashboard(page)
    await page.goto('/start-day')

    // Wait for the block to appear in the plan (confirms existingBlocks loaded).
    // Use the TimeBlock's draggable container rather than the inner clipped text span.
    const planSection = page.locator('section').filter({ hasText: "Today's Plan" })
    await expect(planSection.getByRole('button', { name: /Write tests/ }).first()).toBeVisible()

    await page.getByRole('button', { name: 'Confirm plan' }).click()

    await expect(page).toHaveURL('/')
    await expect(page.getByText('Plan saved. Good luck today!')).toBeVisible()
  })

  test('mid-day replanning pre-populates calendar with existing blocks', async ({ page }) => {
    await mockSuggestedTasks(page, [])
    await mockScheduleToday(page, BLOCKS)
    await page.goto('/start-day')

    // Both existing blocks should appear in the "Today's Plan" grid.
    // Use the TimeBlock's draggable container rather than the inner clipped text span.
    const planSection = page.locator('section').filter({ hasText: "Today's Plan" })
    await expect(planSection.getByRole('button', { name: /Write tests/ }).first()).toBeVisible()
    await expect(planSection.getByRole('button', { name: /Review PR/ }).first()).toBeVisible()
  })

  test('hour range dropdowns default to 8 AM and 5 PM', async ({ page }) => {
    await mockSuggestedTasks(page, [])
    await mockScheduleToday(page, [])
    await mockEventsForDate(page, [])
    await page.goto('/start-day')

    const planSection = page.locator('section').filter({ hasText: "Today's Plan" })
    const selects = planSection.locator('select')

    await expect(selects.first()).toHaveValue('8')
    await expect(selects.last()).toHaveValue('17')
  })

  test('changing start hour updates grid labels', async ({ page }) => {
    await mockSuggestedTasks(page, [])
    await mockScheduleToday(page, [])
    await mockEventsForDate(page, [])
    await page.goto('/start-day')

    const planSection = page.locator('section').filter({ hasText: "Today's Plan" })
    const startSelect = planSection.locator('select').first()

    // Change start to 6 AM
    await startSelect.selectOption('6')

    // Grid should now show 6 AM label — target the span in the hour labels row,
    // not the option element inside the select, to avoid a strict mode violation.
    await expect(planSection.locator('span').filter({ hasText: '6 AM' })).toBeVisible()
  })

  test('narrowing range past existing block shows warning', async ({ page }) => {
    await mockSuggestedTasks(page, TASKS.slice(0, 1))
    // Block at 8:00-9:00 (BLOCKS[0].startTime = '08:00')
    await mockScheduleToday(page, [BLOCKS[0]])
    await mockEventsForDate(page, [])
    await page.goto('/start-day')

    const planSection = page.locator('section').filter({ hasText: "Today's Plan" })

    // Wait for block to appear (use the TimeBlock container, not the clipped inner span)
    await expect(planSection.getByRole('button', { name: /Write tests/ }).first()).toBeVisible()

    // Try to set start hour to 10 AM (would hide the 8:00 block)
    const startSelect = planSection.locator('select').first()
    await startSelect.selectOption('10')

    // Warning should appear and start hour should remain at 8
    await expect(page.getByText(/You have blocks before/)).toBeVisible()
    await expect(startSelect).toHaveValue('8')
  })

  test('drag task card onto calendar adds block', async ({ page }) => {
    await mockSuggestedTasks(page, TASKS.slice(0, 1))
    await mockScheduleToday(page, [])
    await page.goto('/start-day')

    // dnd-kit spreads { role: 'button' } from useDraggable attributes onto the card div
    const taskCard = page.getByRole('button').filter({ hasText: 'Write tests' })
    const planSection = page.locator('section').filter({ hasText: "Today's Plan" })

    // Wait for both elements to be visible before attempting drag
    await expect(taskCard).toBeVisible()
    await expect(planSection).toBeVisible()

    const sourceBox = await taskCard.boundingBox()
    const targetBox = await planSection.boundingBox()

    if (!sourceBox || !targetBox) throw new Error('Could not locate drag source or target')

    const sourceX = sourceBox.x + sourceBox.width / 2
    const sourceY = sourceBox.y + sourceBox.height / 2

    // Use pointer events — dnd-kit PointerSensor requires pointer events (not mouse events)
    await page.mouse.move(sourceX, sourceY)
    await page.mouse.down()

    // Move past the PointerSensor activationConstraint distance (5px)
    await page.mouse.move(sourceX + 10, sourceY)

    // Move to the centre of the time grid
    await page.mouse.move(
      targetBox.x + targetBox.width / 2,
      targetBox.y + targetBox.height / 2,
      { steps: 5 }
    )

    // Release
    await page.mouse.up()

    // Use the TimeBlock container rather than the inner clipped text span
    await expect(planSection.getByRole('button', { name: /Write tests/ }).first()).toBeVisible()
  })
})
