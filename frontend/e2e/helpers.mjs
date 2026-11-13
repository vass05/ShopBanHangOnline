import { expect } from '@playwright/test';
import { randomUUID, createHmac } from 'node:crypto';
import { execFileSync } from 'node:child_process';

export const password = 'IntegrationOnly42!';
export const authorization = user => ({ Authorization: `Bearer ${user.accessToken}` });
export async function data(response, status = 200) {
  expect(response.status()).toBe(status);
  return (await response.json()).data;
}
export async function register(request, name = 'customer') {
  const email = `${name}-${randomUUID()}@example.test`;
  const auth = await data(await request.post('/api/v1/auth/register', { data: { email, password, fullName: 'E2E Customer' } }), 201);
  return { ...auth, password };
}
export async function addAddress(request, user) {
  return data(await request.post('/api/v1/users/me/addresses', { headers: authorization(user), data: {
    recipientName: 'E2E Customer', phone: '0901234567', provinceCity: 'Ha Noi', district: 'Test District', ward: 'Test Ward', streetAddress: '1 Integration Test', isDefault: true,
  } }), 201);
}
export async function login(page, user, remember = true, origin) {
  await page.goto(origin ? new URL('/login', origin).href : '/login');
  await page.locator('form').first().locator('input[type="text"]').fill(user.email);
  await page.locator('form').first().locator('input[type="password"]').fill(user.password);
  await page.locator('form').first().getByRole('checkbox').setChecked(remember);
  const response = page.waitForResponse(result => result.url().endsWith('/auth/login') && result.request().method() === 'POST');
  await page.getByRole('button', { name: 'Đăng Nhập', exact: true }).click();
  expect((await response).status()).toBe(200);
  await expect(page).toHaveURL(/\/$/);
}
export async function browserUser(page) {
  return page.evaluate(() => JSON.parse(sessionStorage.getItem('helishop-session-v3') || localStorage.getItem('helishop-session-v3')));
}
export function sql(query) {
  return execFileSync('docker', ['exec', '-i', 'helishop_integration_mysql', 'sh', '-c',
    'exec mysql --default-character-set=utf8mb4 --batch --skip-column-names -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"'], { input: query, encoding: 'utf8', stdio: ['pipe','pipe','pipe'] }).trim();
}
export function redis(...args) {
  return execFileSync('docker', ['exec', 'helishop_integration_redis', 'redis-cli', '--raw', ...args], { encoding: 'utf8' }).trim();
}
export async function stock(request, id = 9001) {
  return (await data(await request.get(`/api/v1/products/${id}`))).skus[0].stockQuantity;
}
export async function checkout(request, user, address, method = 'VNPAY', items = [{ skuId: 9001, quantity: 1 }]) {
  const body = { addressId: address.id, shippingMethod: 'standard', paymentMethod: method, items };
  const headers = { ...authorization(user), 'Idempotency-Key': randomUUID() };
  const response = await request.post('/api/v1/orders/checkout-batch', { headers, data: body });
  return { orders: await data(response, 201), body, headers };
}
export function signedPayment(order, overrides = {}) {
  const fields = { vnp_TmnCode: 'E2E0001', vnp_TxnRef: order.orderCode, vnp_Amount: String(Number(order.finalAmount) * 100),
    vnp_ResponseCode: '00', vnp_TransactionStatus: '00', vnp_TransactionNo: String(Date.now()), vnp_PayDate: '20261113150000', ...overrides };
  const encoded = Object.keys(fields).sort().map(key => `${key}=${new URLSearchParams([[key, fields[key]]]).toString().split('=').slice(1).join('=')}`).join('&');
  return { ...fields, vnp_SecureHash: createHmac('sha512', 'integration-only-vnpay-secret-not-for-production').update(encoded).digest('hex') };
}
export async function waitMail(request, recipient, subject) {
  let found;
  await expect.poll(async () => {
    const response = await request.get('http://localhost:18025/api/v1/messages');
    expect(response.ok()).toBeTruthy();
    const body = await response.json();
    found = body.messages.find(message => message.To?.some(to => to.Address === recipient) && message.Subject.includes(subject));
    return !!found;
  }, { timeout: 30_000 }).toBeTruthy();
  return (await request.get(`http://localhost:18025/api/v1/message/${found.ID}`)).json();
}
