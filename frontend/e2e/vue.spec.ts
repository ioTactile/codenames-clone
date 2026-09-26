import { test, expect } from '@playwright/test'

test('home page shows brand and create room entry', async ({ page }) => {
  await page.goto('/')
  await expect(page.locator('body')).toBeVisible()
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
})

test('create room route renders join form', async ({ page }) => {
  await page.goto('/room/create')
  await expect(page.getByPlaceholder(/pseudo/i).or(page.locator('input'))).toBeVisible()
})
