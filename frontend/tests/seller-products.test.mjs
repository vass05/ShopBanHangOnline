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
const { sellerProductService: service } = await loadSource('services/sellerProductService.ts');
const session = { user: { id: 8, role: 'ROLE_SELLER' }, accessToken: 'access', refreshToken: 'refresh', rememberMe: false };
const response = (config, data) => ({ config, status: 200, statusText: 'OK', headers: {}, data: { data } });
const metadata = { name: 'My product', categoryId: 3, status: 'DRAFT' };
const sku = { skuCode: 'SKU-1', price: 50000, stockQuantity: 10, attributes: { Color: 'Red' }, version: 4 };
test.beforeEach(() => auth.writeAuthSession(session));

test('seller listing preserves private filters and uses the current-session endpoint without a shop override', async () => {
  const page = { items: [], totalElements: 0, page: 2, size: 20 };
  transport = async config => {
    assert.equal(config.url, '/seller/products');
    assert.deepEqual(config.params, { page: 2, size: 20, keyword: 'red', status: 'DRAFT' });
    assert.equal(config.headers.Authorization, 'Bearer access');
    return response(config, page);
  };
  assert.deepEqual(await service.list({ page: 2, keyword: 'red', status: 'DRAFT', shopId: 999 }), page);
});

test('create sends atomic product and SKU payload while stripping ownership and aggregate fields', async () => {
  transport = async config => {
    assert.equal(config.method, 'post'); assert.equal(config.url, '/seller/products');
    assert.deepEqual(JSON.parse(config.data), { product: metadata, skus: [sku] });
    return response(config, { id: 9, price: 50000, stockQuantity: 10 });
  };
  assert.equal((await service.create({ ...metadata, shopId: 999, rating: 5, stockQuantity: 999 }, [{ ...sku, productId: 999 }])).stockQuantity, 10);
});

test('metadata and SKU edits target separate endpoints and preserve the optimistic version', async () => {
  transport = async config => {
    assert.equal(config.method, 'put');
    assert.equal(config.url, '/seller/products/9');
    assert.deepEqual(JSON.parse(config.data), metadata);
    return response(config, { id: 9 });
  };
  await service.update(9, { ...metadata, price: 1, stockQuantity: 999 });
  transport = async config => {
    assert.equal(config.method, 'put'); assert.equal(config.url, '/seller/products/9/skus/11');
    assert.deepEqual(JSON.parse(config.data), sku);
    return response(config, { id: 9, skus: [{ id: 11, version: 5 }] });
  };
  assert.equal((await service.saveSku(9, 11, sku)).skus[0].version, 5);
  transport = async config => { assert.equal(config.method, 'post'); assert.equal(config.url, '/seller/products/9/skus'); return response(config, { id: 9 }); };
  await service.saveSku(9, null, sku);
});

test('stale SKU version and denied reads are returned as errors for the editor to reload', async () => {
  transport = async config => { throw new AxiosError('Conflict', 'ERR_BAD_REQUEST', config, null, { config, status: 409, data: { message: 'SKU đã thay đổi' }, headers: {}, statusText: 'Conflict' }); };
  await assert.rejects(service.saveSku(9, 11, sku), error => error.response.status === 409);
  await assert.rejects(service.detail(9));
});

test('a queued write from the previous seller cannot be sent with the next account credentials', async () => {
  let sent = 0;
  transport = async config => { sent++; return response(config, {}); };
  const pending = service.saveSku(9, 11, sku);
  auth.writeAuthSession({ ...session, user: { id: 99 }, refreshToken: 'other-session' });
  await assert.rejects(pending); assert.equal(sent, 0);
});
