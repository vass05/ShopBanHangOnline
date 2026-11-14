import { test, expect } from '@playwright/test';
import { randomUUID } from 'node:crypto';
import { register, login, browserUser, authorization, data, sql, password } from './helpers.mjs';

test('seller signup opens one persisted shop, reload and login return to it', async ({ page, request }) => {
  const email = `seller-ui-${randomUUID()}@example.test`;
  const shopName = `Gian hàng ${randomUUID()}`;
  await page.goto('/');
  await page.getByRole('link', { name: 'Kênh Người Bán', exact: true }).click();
  await expect(page).toHaveURL(/\/register\?role=seller$/);
  const form = page.locator('form').first();
  await form.locator('input[type="text"]').fill(email);
  await form.locator('input[type="password"]').nth(0).fill(password);
  await form.locator('input[type="password"]').nth(1).fill(password);
  await page.getByRole('button', { name: /Đăng Ký Mở Gian Hàng/ }).click();
  await expect(page).toHaveURL(/\/seller\/shop$/);
  await page.getByLabel('Tên gian hàng', { exact: true }).fill(shopName);
  await page.getByLabel('Mô tả gian hàng (không bắt buộc)', { exact: true }).fill('Gian hàng thử nghiệm\nDữ liệu lưu thật.');
  const opened = page.waitForResponse(response => response.url().endsWith('/shops/me') && response.request().method() === 'POST');
  await page.getByRole('button', { name: 'Mở gian hàng', exact: true }).click();
  const shop = await data(await opened, 201);
  const session = await browserUser(page);
  expect(shop.ownerId).toBe(session.user.id);
  expect(shop.status).toBe('ACTIVE');
  await expect(page.getByRole('heading', { name: shopName, exact: true })).toBeVisible();
  await page.reload();
  await expect(page.getByRole('heading', { name: shopName, exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Mở gian hàng', exact: true })).toHaveCount(0);
  expect(sql(`SELECT COUNT(*) FROM shops WHERE owner_id=${Number(session.user.id)};`)).toBe('1');
  expect((await request.post('/api/v1/shops/me', { headers: authorization(session), data: { shopName: `Other ${randomUUID()}` } })).status()).toBe(409);
  // Logout through the existing profile control, then exercise the default seller login destination.
  await page.goto('/profile');
  await page.getByRole('button', { name: 'Đăng xuất', exact: true }).click();
  await login(page, { email, password, role: 'ROLE_SELLER' });
  await expect(page.getByRole('heading', { name: shopName, exact: true })).toBeVisible();
});

test('shop name conflict stays editable, customer denied and another seller sees no private shop', async ({ page, request }) => {
  const first = await register(request, 'seller-first', 'ROLE_SELLER');
  const second = await register(request, 'seller-second', 'ROLE_SELLER');
  const name = `Shop-${randomUUID()}`;
  await data(await request.post('/api/v1/shops/me', { headers: authorization(first), data: { shopName: name } }), 201);
  expect(await data(await request.get('/api/v1/shops/me', { headers: authorization(second) }))).toBeUndefined();
  await login(page, second);
  await page.getByLabel('Tên gian hàng', { exact: true }).fill(name.toUpperCase());
  await page.getByRole('button', { name: 'Mở gian hàng', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('Tên gian hàng đã được sử dụng');
  await page.getByLabel('Tên gian hàng', { exact: true }).fill(`${name}-unique`);
  await page.getByRole('button', { name: 'Mở gian hàng', exact: true }).click();
  await expect(page.getByRole('heading', { name: `${name}-unique`, exact: true })).toBeVisible();
  const customer = await register(request, 'shop-customer');
  expect((await request.get('/api/v1/shops/me', { headers: authorization(customer) })).status()).toBe(403);
  expect((await request.post('/api/v1/shops/me', { headers: authorization(customer), data: { shopName: name } })).status()).toBe(403);
  await page.goto('/profile'); await page.getByRole('button', { name: 'Đăng xuất', exact: true }).click();
  await login(page, customer); await page.goto('/seller/shop');
  await expect(page.getByRole('heading', { name: 'Chức năng dành cho tài khoản người bán' })).toBeVisible();
  await expect(page.getByLabel('Tên gian hàng', { exact: true })).toHaveCount(0);
});

test('MySQL serializes concurrent opens for one account and arbitrates duplicate shop names', async ({ request }) => {
  const owner = await register(request, 'seller-race', 'ROLE_SELLER');
  const create = (user, shopName) => request.post('/api/v1/shops/me', { headers: authorization(user), data: { shopName } });
  const sameOwner = await Promise.all(Array.from({ length: 4 }, () => create(owner, `Shop-${randomUUID()}`)));
  expect(sameOwner.map(response => response.status()).sort()).toEqual([201, 409, 409, 409]);
  expect(sql(`SELECT COUNT(*) FROM shops WHERE owner_id=${Number(owner.userId)};`)).toBe('1');
  const sellers = await Promise.all([register(request, 'name-race-a', 'ROLE_SELLER'), register(request, 'name-race-b', 'ROLE_SELLER')]);
  const name = `Shop-${randomUUID()}`;
  const sameName = await Promise.all(sellers.map(user => create(user, name)));
  expect(sameName.map(response => response.status()).sort()).toEqual([201, 409]);
  const losingSeller = sellers[sameName.findIndex(response => response.status() === 409)];
  expect((await create(losingSeller, `${name}-retry`)).status()).toBe(201);
});

test('failed shop reads require retry and a shop created in another tab is recovered after conflict', async ({ page, request }) => {
  const seller = await register(request, 'seller-retry', 'ROLE_SELLER');
  await page.route('**/api/v1/shops/me', route => route.request().method() === 'GET'
    ? route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ message: 'Tạm thời không thể tải shop' }) })
    : route.continue());
  await login(page, seller);
  await expect(page.getByRole('alert')).toContainText('Tạm thời không thể tải shop');
  await expect(page.getByLabel('Tên gian hàng', { exact: true })).toHaveCount(0);
  await page.unroute('**/api/v1/shops/me');
  await page.getByRole('button', { name: 'Tải lại gian hàng' }).click();
  await page.getByLabel('Tên gian hàng', { exact: true }).fill('Tên cũ trên form');
  const actualName = `Shop other tab-${randomUUID()}`;
  await data(await request.post('/api/v1/shops/me', { headers: authorization(seller), data: { shopName: actualName } }), 201);
  await page.getByRole('button', { name: 'Mở gian hàng', exact: true }).click();
  await expect(page.getByRole('heading', { name: actualName, exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Mở gian hàng', exact: true })).toHaveCount(0);
});
