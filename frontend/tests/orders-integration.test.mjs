import test from 'node:test';
import assert from 'node:assert/strict';
import axios, { AxiosError } from 'axios';
import { loadSource } from './load-source.mjs';
const values = new Map();
globalThis.sessionStorage = { getItem: key => values.get(key) ?? null, setItem: (key, value) => values.set(key, value), removeItem: key => values.delete(key) };
globalThis.localStorage = { getItem: () => null, setItem: () => {}, removeItem: () => {} };
let transport;
axios.defaults.adapter = config => transport(config);
const auth = await loadSource('lib/authSession.ts');
const { useOrderStore: store } = await loadSource('store/useOrderStore.ts');
const session = { user: { id: 1 }, accessToken: 'access', refreshToken: 'refresh', rememberMe: false };
const order = { id: 10, orderCode: 'ORD-SERVER', orderStatus: 'PENDING', paymentStatus: 'UNPAID', finalAmount: 130000, items: [] };
const response = (config, data) => ({ config, status: 200, statusText: 'OK', headers: {}, data: { data } });
test.beforeEach(async () => {
  transport = config => Promise.resolve(response(config, [order]));
  auth.writeAuthSession(session);
  await store.getState().fetchOrders();
});

test('history loads from the current-user endpoint and cancellation displays server state', async () => {
  transport = config => {
    if (config.method === 'patch') {
      assert.equal(config.url, '/orders/10/status');
      assert.equal(config.params.status, 'CANCELLED');
      return Promise.resolve(response(config, { ...order, orderStatus: 'CANCELLED' }));
    }
    assert.equal(config.url, '/orders/me');
    return Promise.resolve(response(config, [order]));
  };
  await store.getState().fetchOrders();
  assert.equal(store.getState().orders[0].orderCode, 'ORD-SERVER');
  assert.equal(await store.getState().cancelOrder(10), true);
  assert.equal(store.getState().orders[0].orderStatus, 'CANCELLED');
});

test('failed cancellation preserves the confirmed server status and reports failure', async () => {
  transport = config => Promise.reject(new AxiosError('Conflict', 'ERR_BAD_REQUEST', config, null, { status: 409, data: { message: 'Không thể hủy' }, headers: {}, config, statusText: 'Conflict' }));
  assert.equal(await store.getState().cancelOrder(10), false);
  assert.equal(store.getState().orders[0].orderStatus, 'PENDING');
  assert.equal(store.getState().error, 'Không thể hủy');
});

test('logout clears cached history and stale responses cannot restore it', async () => {
  let finish;
  transport = config => new Promise(resolve => { finish = () => resolve(response(config, [order])); });
  const pending = store.getState().fetchOrders();
  await new Promise(resolve => setTimeout(resolve, 0));
  auth.writeAuthSession(null);
  assert.equal(store.getState().orders.length, 0);
  finish(); await pending;
  assert.equal(store.getState().orders.length, 0);
});
