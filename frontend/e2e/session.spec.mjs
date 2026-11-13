import { test, expect } from '@playwright/test';
import { register, login, browserUser, authorization, data } from './helpers.mjs';

test('same-origin login preserves a non-default port for a host outside the CORS allowlist', async ({ page, request }) => {
  const user = await register(request, 'origin');
  await login(page, user, true, 'http://127.0.0.1:18000');
  await expect(page).toHaveURL('http://127.0.0.1:18000/');
});

test('real refresh restores an invalid access token; session-only login and logout stay isolated', async ({ page, context, request }) => {
  const user = await register(request, 'session');
  await login(page, user, false);
  expect(await page.evaluate(() => localStorage.getItem('helishop-session-v3'))).toBeNull();
  await page.evaluate(() => {
    const session = JSON.parse(sessionStorage.getItem('helishop-session-v3'));
    session.accessToken = 'invalid-access-token-for-refresh-test';
    sessionStorage.setItem('helishop-session-v3', JSON.stringify(session));
  });
  const refresh = page.waitForResponse(response => response.url().endsWith('/auth/refresh-token'));
  await page.goto('/profile');
  expect((await refresh).status()).toBe(200);
  await expect.poll(async () => (await browserUser(page)).accessToken).not.toBe('invalid-access-token-for-refresh-test');
  const session = await browserUser(page);
  expect((await request.get('/api/v1/users/me', { headers: authorization(session) })).status()).toBe(200);
  const independentTab = await context.newPage();
  await independentTab.goto('/orders');
  await expect(independentTab).toHaveURL(/\/login/);
  await independentTab.close();
  // The profile sidebar exposes logout without relying on hover menus.
  await page.getByRole('button', { name: 'Đăng xuất', exact: true }).click();
  await expect(page).toHaveURL(/\/login/);
  expect(await page.evaluate(() => sessionStorage.getItem('helishop-session-v3'))).toBeNull();
  expect((await request.post('/api/v1/auth/refresh-token', { data: { refreshToken: session.refreshToken } })).status()).toBe(401);
  const stranger = await register(request, 'isolation');
  expect(await data(await request.get('/api/v1/orders/me', { headers: authorization(stranger) }))).toEqual([]);
});
