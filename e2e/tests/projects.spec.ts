import { test, expect } from '../fixtures/auth'

const PROJECTS = [
  {
    id: 'proj-1',
    name: 'Work',
    description: 'Day-to-day work tasks',
    color: '#6366f1',
    icon: '💼',
    isActive: true,
    createdAt: '2026-03-01T00:00:00Z',
    updatedAt: '2026-03-01T00:00:00Z',
  },
  {
    id: 'proj-2',
    name: 'Personal',
    description: null,
    color: '#22c55e',
    icon: null,
    isActive: true,
    createdAt: '2026-03-02T00:00:00Z',
    updatedAt: '2026-03-02T00:00:00Z',
  },
]

const TASKS = [
  {
    id: 'task-1',
    projectId: 'proj-1',
    title: 'Write tests',
    parentTaskId: null,
    status: 'OPEN',
    visibleFrom: null,
    archivedAt: null,
  },
  {
    id: 'task-2',
    projectId: 'proj-1',
    title: 'Review PR',
    parentTaskId: null,
    status: 'OPEN',
    visibleFrom: null,
    archivedAt: null,
  },
  {
    id: 'task-3',
    projectId: 'proj-1',
    title: 'Update docs',
    parentTaskId: null,
    status: 'OPEN',
    visibleFrom: null,
    archivedAt: null,
  },
]

async function mockProjectsPage(page: import('@playwright/test').Page) {
  await page.route('**/api/v1/projects', route =>
    route.fulfill({ json: PROJECTS })
  )
  // Card UI fetches tasks inline for each project
  await page.route('**/api/v1/projects/proj-1/tasks', route =>
    route.fulfill({ json: TASKS })
  )
  await page.route('**/api/v1/projects/proj-2/tasks', route =>
    route.fulfill({ json: [] })
  )
}

test.describe('Projects list (/projects)', () => {
  test('shows active projects', async ({ page }) => {
    await mockProjectsPage(page)
    await page.goto('/projects')

    await expect(page.getByRole('heading', { name: 'Projects' })).toBeVisible()
    await expect(page.getByText('Work', { exact: true })).toBeVisible()
    await expect(page.getByText('Personal', { exact: true })).toBeVisible()
    await expect(page.getByText('Day-to-day work tasks')).toBeVisible()
  })

  test('shows task previews on project cards', async ({ page }) => {
    await mockProjectsPage(page)
    await page.goto('/projects')

    // Work project card shows its active tasks
    await expect(page.getByText('Write tests')).toBeVisible()
    await expect(page.getByText('Review PR')).toBeVisible()
    await expect(page.getByText('Update docs')).toBeVisible()

    // Personal project card shows empty state
    await expect(page.getByText('No active tasks')).toBeVisible()
  })

  test('shows action buttons on project cards', async ({ page }) => {
    await mockProjectsPage(page)
    await page.goto('/projects')

    await expect(page.getByRole('button', { name: 'Add task to Work' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Edit Work' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Archive Work' })).toBeVisible()
  })

  test('shows New project button', async ({ page }) => {
    await mockProjectsPage(page)
    await page.goto('/projects')

    await expect(page.getByRole('button', { name: 'New project' })).toBeVisible()
  })
})
