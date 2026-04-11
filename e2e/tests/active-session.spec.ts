import { test, expect } from '../fixtures/auth'
import {
  mockSessionBlock,
  mockTaskDetail,
  mockSessionEndpoints,
  mockDashboard,
} from '../fixtures/mocks'

test.describe('Active Work Session', () => {
  test.beforeEach(async ({ page }) => {
    await mockSessionBlock(page)
    await mockTaskDetail(page)
    await mockSessionEndpoints(page)
  })

  test('shows task title and project name', async ({ page }) => {
    await page.goto('/session/block-session-1')
    await expect(page.getByText('Work', { exact: true })).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Write tests' })).toBeVisible()
  })

  test('shows countdown timer', async ({ page }) => {
    await page.goto('/session/block-session-1')
    await expect(page.getByRole('main').getByText(/\d{2}:\d{2}/)).toBeVisible()
    await expect(page.getByText(/of \d+ min/)).toBeVisible()
  })

  test('shows subtask checklist when task has children', async ({ page }) => {
    await page.goto('/session/block-session-1')
    await expect(page.getByText('Subtasks')).toBeVisible()
    await expect(page.getByText('Unit tests')).toBeVisible()
    await expect(page.getByText('Integration tests')).toBeVisible()
    await expect(page.getByText('E2E tests')).toBeVisible()
  })

  test('hides subtask section when task has no children', async ({ page }) => {
    await mockTaskDetail(page, { id: 'task-1', title: 'Write tests', children: [] })
    await page.goto('/session/block-session-1')
    await expect(page.getByRole('heading', { name: 'Write tests' })).toBeVisible()
    await expect(page.getByText('Subtasks')).not.toBeVisible()
  })

  test('complete button navigates back with flash', async ({ page }) => {
    await mockDashboard(page)
    await page.goto('/session/block-session-1')
    await page.getByRole('button', { name: 'Complete' }).click()
    await expect(page.getByText('Nice work!')).toBeVisible()
  })

  test('done for now navigates back', async ({ page }) => {
    await mockDashboard(page)
    await page.goto('/session/block-session-1')
    await page.getByRole('button', { name: 'Done for now' }).click()
    await expect(page).not.toHaveURL(/\/session\//)
  })

  test('extend shows duration options', async ({ page }) => {
    await page.goto('/session/block-session-1')
    await page.getByRole('button', { name: 'Extend' }).click()
    await expect(page.getByText('15 min')).toBeVisible()
    await expect(page.getByText('30 min')).toBeVisible()
    await expect(page.getByText('60 min')).toBeVisible()
  })

  test('action buttons are present', async ({ page }) => {
    await page.goto('/session/block-session-1')
    await expect(page.getByRole('button', { name: 'Complete' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Extend' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Done for now' })).toBeVisible()
  })
})
