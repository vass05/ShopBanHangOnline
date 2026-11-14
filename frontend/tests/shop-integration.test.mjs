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
const { shopService } = await loadSource('services/shopService.ts');
const session = { user: { id: 7, role: 'ROLE_SELLER' }, accessToken: 'access', refreshToken: 'refresh', rememberMe: false };
const shop = { id: 10, ownerId: 7, shopName: 'Shop thật', status: 'ACTIVE' };
const response = (config, data) => ({ config, status: 200, statusText: 'OK', headers: {}, data: { data } });
test.beforeEach(() => auth.writeAuthSession(session));

test('own shop reads the authenticated endpoint and empty envelope means no shop', async () => {
  for (const value of [undefined, null, shop]) {
    transport = async config => {
      assert.equal(config.url, '/shops/me');
      assert.equal(config.method, 'get');
      assert.equal(config.headers.Authorization, 'Bearer access');
      return response(config, value);
    };
    assert.deepEqual(await shopService.mine(), value ?? null);
  }
});

test('opening sends only shop information and uses confirmed server identity/status', async () => {
  transport = async config => {
    assert.equal(config.url, '/shops/me'); assert.equal(config.method, 'post');
    assert.deepEqual(JSON.parse(config.data), { shopName: 'Shop thật', description: 'Mô tả' });
    return response(config, shop);
  };
  assert.deepEqual(await shopService.open({ shopName: 'Shop thật', description: 'Mô tả', ownerId: 999, status: 'BANNED' }), shop);
});

test('name conflicts and read failures propagate instead of fabricating a shop', async () => {
  transport = async config => { throw new AxiosError('Conflict', 'ERR_BAD_REQUEST', config, null,
    { status: 409, data: { message: 'Tên gian hàng đã được sử dụng' }, config, headers: {}, statusText: 'Conflict' }); };
  await assert.rejects(shopService.open({ shopName: 'Trùng tên' }), error => error.response.status === 409);
  await assert.rejects(shopService.mine(), error => error.response.status === 409);
});

test('a shop request captured for an old session cannot be sent as the next seller', async () => {
  let sent = 0;
  transport = async config => { sent++; return response(config, shop); };
  const pending = shopService.open({ shopName: 'Old session' });
  auth.writeAuthSession({ ...session, user: { id: 8, role: 'ROLE_SELLER' }, refreshToken: 'new-refresh' });
  await assert.rejects(pending);
  assert.equal(sent, 0);
});
