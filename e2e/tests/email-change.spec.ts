import { test, expect } from '@playwright/test'

// The verify-email page is public (reached from a link in the verification
// email), so it uses the raw test runner rather than the auth fixture.

test.describe('Email change confirmation', () => {
  test('valid token confirms the change and offers a way back to log in', async ({ page }) => {
    await page.route('**/api/v1/auth/email/confirm', route =>
      route.fulfill({ json: { email: 'new@example.com' } })
    )
    await page.route('**/api/v1/auth/logout', route => route.fulfill({ status: 204, body: '' }))

    await page.goto('/verify-email?token=good-token')

    await expect(page.getByRole('heading', { name: 'Email updated' })).toBeVisible()
    await expect(page.getByText(/new@example\.com/)).toBeVisible()

    await page.getByRole('button', { name: 'Log in' }).click()
    await expect(page).toHaveURL(/\/login$/)
  })

  test('invalid or expired token shows an error with a link back to log in', async ({ page }) => {
    await page.route('**/api/v1/auth/email/confirm', route =>
      route.fulfill({ status: 400, json: { detail: 'This verification link has expired' } })
    )

    await page.goto('/verify-email?token=bad-token')

    await expect(page.getByRole('heading', { name: "This link didn't work" })).toBeVisible()
    await expect(page.getByRole('link', { name: 'Back to log in' })).toBeVisible()
  })

  test('missing token shows the error state without calling the API', async ({ page }) => {
    let called = false
    await page.route('**/api/v1/auth/email/confirm', route => {
      called = true
      route.fulfill({ json: { email: 'x@example.com' } })
    })

    await page.goto('/verify-email')

    await expect(page.getByRole('heading', { name: "This link didn't work" })).toBeVisible()
    expect(called).toBe(false)
  })
})
