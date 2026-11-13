import { defineConfig, devices } from '@playwright/test';
const baseURL = process.env.E2E_BASE_URL || 'http://localhost:18000';
if (!/^http:\/\/(localhost|127\.0\.0\.1):18000$/.test(baseURL)) {
  throw new Error('These tests require the isolated local Docker project prepared by scripts/Prepare-Integration.ps1.');
}

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  forbidOnly: !!process.env.CI,
  retries: 0,
  timeout: 90_000,
  expect: { timeout: 15_000 },
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    ...devices['Desktop Chrome'],
    baseURL,
    channel: process.env.E2E_BROWSER_CHANNEL || undefined,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
});
