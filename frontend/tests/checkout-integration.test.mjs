import test from 'node:test';
import assert from 'node:assert/strict';
import axios from 'axios';
import { loadSource } from './load-source.mjs';
const values = new Map();
globalThis.sessionStorage = { getItem: key => values.get(key) ?? null, setItem: (key, value) => values.set(key, value), removeItem: key => values.delete(key) };
globalThis.localStorage = { getItem: () => null, setItem: () => {}, removeItem: () => {} };
let transport;
axios.defaults.adapter = config => transport(config);
const { checkoutAttemptKey, checkoutService } = await loadSource('services/checkoutService.ts');
const { writeAuthSession } = await loadSource('lib/authSession.ts');
const payload = { addressId: 5, shippingMethod: 'standard', paymentMethod: 'COD', items: [{ skuId: 10, quantity: 2 }] };
test.beforeEach(() => values.clear());

test('retry retains idempotency key, while another payload or account gets another key', () => {
  const key = checkoutAttemptKey(1, payload);
  assert.equal(checkoutAttemptKey(1, payload), key);
  assert.notEqual(checkoutAttemptKey(2, payload), key);
  assert.notEqual(checkoutAttemptKey(1, { ...payload, shippingMethod: 'express' }), key);
  assert.match(key, /^[a-f0-9]{32}$/);
});

test('checkout sends real SKU quantities and idempotency header and uses only server-created orders', async () => {
  writeAuthSession({ user: { id: 1 }, accessToken: 'access', refreshToken: 'refresh', rememberMe: false });
  const actual = [{ id: 123, orderCode: 'ORD-SERVER', finalAmount: 230000, paymentStatus: 'UNPAID' }];
  transport = async config => {
    assert.equal(config.url, '/orders/checkout-batch');
    assert.equal(config.headers['Idempotency-Key'], 'checkout-request-0001');
    const data = JSON.parse(config.data);
    assert.deepEqual(data.items, [{ skuId: 10, quantity: 2 }]);
    assert.equal(data.totalAmount, undefined);
    assert.equal(data.customerId, undefined);
    return { config, status: 201, statusText: 'Created', headers: {}, data: { data: actual } };
  };
  assert.deepEqual(await checkoutService.create(payload, 'checkout-request-0001'), actual);
});

test('checkout failure propagates without manufacturing a local order', async () => {
  writeAuthSession({ user: { id: 1 }, accessToken: 'access', refreshToken: 'refresh', rememberMe: false });
  transport = async () => { throw new Error('network unavailable'); };
  await assert.rejects(checkoutService.create(payload, 'checkout-request-0001'), /network unavailable/);
});
