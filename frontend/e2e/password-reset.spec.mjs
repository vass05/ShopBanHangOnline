import { test, expect } from '@playwright/test';
import { register, waitMail, login } from './helpers.mjs';

test('OTP arrives in captured SMTP, is absent from API, single-use and revokes the prior refresh token', async ({ page, request }) => {
  const user = await register(request, 'otp');
  const response = await request.post('/api/v1/auth/forgot-password', { data: { email: user.email } });
  expect(response.status()).toBe(200);
  const mail = await waitMail(request, user.email, 'khôi phục mật khẩu');
  const otp = mail.Text.match(/: (\d{6})\b/)?.[1];
  expect(otp).toMatch(/^\d{6}$/);
  expect(await response.text()).not.toContain(otp);
  expect((await request.post('/api/v1/auth/forgot-password', { data: { email: user.email } })).status()).toBe(429);
  const newPassword = 'ChangedIntegrationOnly42!';
  const reset = { email: user.email, otp, newPassword };
  expect((await request.post('/api/v1/auth/reset-password', { data: reset })).status()).toBe(200);
  expect((await request.post('/api/v1/auth/reset-password', { data: reset })).status()).toBe(400);
  expect((await request.post('/api/v1/auth/login', { data: { email: user.email, password: user.password } })).status()).toBe(401);
  expect((await request.post('/api/v1/auth/refresh-token', { data: { refreshToken: user.refreshToken } })).status()).toBe(401);
  await login(page, { ...user, password: newPassword });
});
