import test from 'node:test';
import assert from 'node:assert/strict';
import axios from 'axios';
import { loadSource } from './load-source.mjs';
const values = new Map();
globalThis.sessionStorage = { getItem: key => values.get(key) ?? null, setItem: (key, value) => values.set(key, value), removeItem: key => values.delete(key) };
globalThis.localStorage = { getItem: () => null, setItem: () => {}, removeItem: () => {} };
let transport;
axios.defaults.adapter = config => transport(config);
const { paymentView, paymentService } = await loadSource('services/paymentService.ts');
const { writeAuthSession } = await loadSource('lib/authSession.ts');
const order = { id: 10, orderCode: 'ORD-SERVER', orderStatus: 'PENDING', paymentStatus: 'UNPAID', paymentMethod: 'VNPAY', finalAmount: 130000 };

test('successful browser return is pending until server order is PAID', () => {
  assert.equal(paymentView(order, { verified: true, status: 'PENDING', responseCode: '00' }), 'pending');
  assert.equal(paymentView({ ...order, paymentStatus: 'PAID' }), 'paid');
  assert.equal(paymentView(order, { verified: false, status: 'FAILED' }), 'invalid');
});

test('closed order is not presented as fulfilled after a late payment', () => {
  assert.equal(paymentView({ ...order, orderStatus: 'CANCELLED', paymentStatus: 'PAID' }), 'closed-paid');
  assert.equal(paymentView({ ...order, orderStatus: 'CANCELLED' }), 'cancelled');
  assert.equal(paymentView({ ...order, paymentMethod: 'COD' }), 'placed');
});

test('payment request uses real orderCode and rejects a missing or unsafe URL', async () => {
  writeAuthSession({ user: { id: 1 }, accessToken: 'access', refreshToken: 'refresh', rememberMe: false });
  transport = async config => {
    assert.deepEqual(JSON.parse(config.data), { orderCode: 'ORD-SERVER' });
    return { config, status: 200, statusText: 'OK', headers: {}, data: { data: { paymentUrl: 'javascript:alert(1)' } } };
  };
  await assert.rejects(paymentService.createUrl('ORD-SERVER'), /URL/);
});

test('return verification uses backend response in VND without interpreting raw query amounts', async () => {
  transport = async config => {
    assert.equal(config.url, '/payments/vnpay-return');
    assert.equal(config.params.vnp_Amount, '13000000');
    return { config, status: 200, statusText: 'OK', headers: {}, data: { data: { verified: true, status: 'PENDING', amount: 130000 } } };
  };
  const result = await paymentService.verifyReturn({ vnp_Amount: '13000000', vnp_SecureHash: 'signed' });
  assert.equal(result.amount, 130000);
  assert.equal(result.status, 'PENDING');
});
