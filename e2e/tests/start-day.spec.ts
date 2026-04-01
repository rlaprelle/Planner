import { test, expect } from '../fixtures/auth'
import { mockSuggestedTasks, mockScheduleToday, mockSavePlan, mockDashboard } from '../fixtures/mocks'
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
    const taskCardDiv = page.getByText('Write tests').locator('..').locator('..')
    await taskCardDiv.locator('input[type="checkbox"]').check()

    await page.getByRole('button', { name: '+ Add to calendar' }).click()

    // Task title should now appear as a TimeBlock in the "Today's Plan" section
    const planSection = page.locator('section').filter({ hasText: "Today's Plan" })
    await expect(planSection.getByText('Write tests')).toBeVisible()
  })

  test('confirm plan navigates to dashboard with toast', async ({ page }) => {
    await mockSuggestedTasks(page, TASKS.slice(0, 1))
    // Pre-populate calendar with one block so "Confirm plan" is enabled
    await mockScheduleToday(page, [BLOCKS[0]])
    await mockSavePlan(page)
    // Dashboard mock needed for the page that loads after navigation
    await mockDashboard(page)
    await page.goto('/start-day')

    // Wait for the block to appear in the plan (confirms existingBlocks loaded)
    const planSection = page.locator('section').filter({ hasText: "Today's Plan" })
    await expect(planSection.getByText('Write tests')).toBeVisible()

    await page.getByRole('button', { name: 'Confirm plan' }).click()

    await expect(page).toHaveURL('/')
    await expect(page.getByText('Plan saved. Good luck today!')).toBeVisible()
  })

  test('mid-day replanning pre-populates calendar with existing blocks', async ({ page }) => {
    await mockSuggestedTasks(page, [])
    await mockScheduleToday(page, BLOCKS)
    await page.goto('/start-day')

    // Both existing blocks should appear in the "Today's Plan" grid
    const planSection = page.locator('section').filter({ hasText: "Today's Plan" })
    await expect(planSection.getByText('Write tests')).toBeVisible()
    await expect(planSection.getByText('Review PR')).toBeVisible()
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

    await expect(planSection.getByText('Write tests')).toBeVisible()
  })
})
