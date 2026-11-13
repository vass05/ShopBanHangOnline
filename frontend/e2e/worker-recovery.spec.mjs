import { test, expect } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import { register, addAddress, checkout, signedPayment, sql } from './helpers.mjs';

const docker = (...args) => execFileSync('docker', args, { encoding: 'utf8', stdio: ['pipe','pipe','pipe'] }).trim();
const deadLetters = () => {
  const output = docker('exec', 'helishop_integration_rabbitmq', 'rabbitmqctl', '-q', 'list_queues', 'name', 'messages_ready');
  const count = output.match(/^order\.email\.dlq\s+(\d+)$/m)?.[1];
  expect(count).toBeDefined();
  return Number(count);
};

test('SMTP failure retries then leaves the paid-order message in DLQ for operator replay', async ({ request }) => {
  expect(docker('inspect', '--format', '{{ index .Config.Labels "com.docker.compose.project" }}', 'helishop_integration_mailpit')).toBe('helishop-integration');
  const user = await register(request, 'dlq');
  const address = await addAddress(request, user);
  const { orders: [order] } = await checkout(request, user, address);
  const before = deadLetters();
  docker('stop', 'helishop_integration_mailpit');
  try {
    const response = await request.get('/api/v1/payments/vnpay-ipn', { params: signedPayment(order) });
    expect((await response.json()).RspCode).toBe('00');
    await expect.poll(deadLetters, { timeout: 35_000, intervals: [1000,2000,3000] }).toBe(before + 1);
    const id = Number(order.id);
    expect(Number.isSafeInteger(id)).toBeTruthy();
    expect(sql(`SELECT payment_status FROM orders WHERE id=${id};`)).toBe('PAID');
    expect(sql(`SELECT COUNT(*) FROM payment_transactions WHERE order_id=${id};`)).toBe('1');
    expect(sql(`SELECT CAST(email_published AS UNSIGNED) FROM order_paid_outbox WHERE order_id=${id};`)).toBe('1');
  } finally {
    docker('start', 'helishop_integration_mailpit');
    await expect.poll(async () => {
      try { return (await request.get('http://localhost:18025/readyz')).status(); } catch { return 0; }
    }).toBe(200);
  }
});
