import { test, expect } from '../fixtures/auth'
import { TASKS } from '../fixtures/data'

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

async function mockProjects(page: import('@playwright/test').Page, projects = PROJECTS) {
  await page.route('**/api/v1/projects', route =>
    route.fulfill({ json: projects })
  )
}

async function mockProjectDetail(
  page: import('@playwright/test').Page,
  project = PROJECTS[0],
  tasks: readonly unknown[] = TASKS
) {
  await page.route(`**/api/v1/projects/${project.id}`, route =>
    route.fulfill({ json: project })
  )
  await page.route(`**/api/v1/projects/${project.id}/tasks`, route =>
    route.fulfill({ json: tasks })
  )
}

test.describe('Projects list (/projects)', () => {
  test('shows active projects', async ({ page }) => {
    await mockProjects(page)
    await page.goto('/projects')

    await expect(page.getByRole('heading', { name: 'Projects' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Work' })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Personal' })).toBeVisible()
    await expect(page.getByText('Day-to-day work tasks')).toBeVisible()
  })

  test('can navigate to project detail by clicking a project', async ({ page }) => {
    await mockProjects(page)
    await mockProjectDetail(page)
    await page.goto('/projects')

    await page.getByRole('link', { name: 'Work' }).click()

    await expect(page).toHaveURL(/\/projects\/proj-1/)
    await expect(page.getByRole('heading', { name: 'Work' })).toBeVisible()
  })
})

test.describe('Project detail (/projects/:id)', () => {
  test('shows project name in header', async ({ page }) => {
    await mockProjectDetail(page)
    await page.goto('/projects/proj-1')

    await expect(page.getByRole('heading', { name: 'Work' })).toBeVisible()
    await expect(page.getByRole('link', { name: '← Projects' })).toBeVisible()
  })

  test("shows project's task list", async ({ page }) => {
    await mockProjectDetail(page)
    await page.goto('/projects/proj-1')

    // All three tasks from the fixture should appear
    await expect(page.getByText('Write tests')).toBeVisible()
    await expect(page.getByText('Review PR')).toBeVisible()
    await expect(page.getByText('Update docs')).toBeVisible()
  })
})
