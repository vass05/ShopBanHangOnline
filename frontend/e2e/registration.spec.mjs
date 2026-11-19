import { test, expect } from '@playwright/test';
import { randomUUID } from 'node:crypto';
import { waitMail, password, sql, redis } from './helpers.mjs';

test('registration requires Gmail OTP, single use, no account before verification, phone is not login', async ({ request }) => {
  const email = `registration-${randomUUID()}@gmail.com`;
  const body = { email, password, role: 'ROLE_CUSTOMER' };
  expect((await request.post('/api/v1/auth/register', { data: body })).status()).toBe(400);
  expect((await request.post('/api/v1/auth/register/otp', { data: { email: '0901234567' } })).status()).toBe(400);
  expect((await request.post('/api/v1/auth/register/otp', { data: { email: 'buyer@example.com' } })).status()).toBe(400);
  const sent = await request.post('/api/v1/auth/register/otp', { data: { email } });
  expect(sent.status()).toBe(200);
  const mail = await waitMail(request, email, 'Xác thực đăng ký');
  const otp = mail.Text.match(/: (\d{6})\b/)[1];
  expect(await sent.text()).not.toContain(otp);
  expect(sql(`SELECT COUNT(*) FROM users WHERE email='${email}'`)).toBe('0');
  expect(redis('GET', `auth:registration:${email}`)).not.toContain(otp);
  expect(Number(redis('TTL', `auth:registration:${email}`))).toBeGreaterThan(0);
  expect((await request.post('/api/v1/auth/register/otp', { data: { email } })).status()).toBe(429);
  expect((await request.post('/api/v1/auth/register', { data: { ...body, otp: 'abcdef' } })).status()).toBe(400);
  const outcomes = await Promise.all([1,2].map(() => request.post('/api/v1/auth/register', { data: { ...body, otp } })));
  expect(outcomes.map(r => r.status()).filter(s => s === 201)).toHaveLength(1);
  expect(sql(`SELECT COUNT(*) FROM users WHERE email='${email}'`)).toBe('1');
  expect(redis('GET', `auth:registration:${email}`)).toBe('');
  expect((await request.post('/api/v1/auth/login', { data: { email: '0901234567', password } })).status()).toBe(400);
});

test('five wrong OTPs invalidate registration code even if the correct code is supplied later', async ({ request }) => {
  const email = `otp-attempts-${randomUUID()}@gmail.com`;
  expect((await request.post('/api/v1/auth/register/otp', { data: { email } })).status()).toBe(200);
  const mail = await waitMail(request, email, 'Xác thực đăng ký');
  const otp = mail.Text.match(/: (\d{6})\b/)[1];
  const wrong = otp === '000000' ? '999999' : '000000';
  for (let i = 0; i < 5; i++) expect((await request.post('/api/v1/auth/register', { data: { email, password, otp: wrong } })).status()).toBe(400);
  expect((await request.post('/api/v1/auth/register', { data: { email, password, otp } })).status()).toBe(400);
  expect(sql(`SELECT COUNT(*) FROM users WHERE email='${email}'`)).toBe('0');
});
