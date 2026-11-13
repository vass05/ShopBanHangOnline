import test from 'node:test';
import assert from 'node:assert/strict';
import axios, { AxiosError } from 'axios';
import { loadSource } from './load-source.mjs';
const memoryStorage = () => {
  const values = new Map();
  return { getItem: key => values.get(key) ?? null, setItem: (key, value) => values.set(key, value), removeItem: key => values.delete(key), clear: () => values.clear() };
};
globalThis.localStorage = memoryStorage();
globalThis.sessionStorage = memoryStorage();
globalThis.window = { location: { href: '' } };
let serverItems = [];
let transport;
axios.defaults.adapter = config => transport(config);
const { mapCart } = await loadSource('services/cartService.ts');
const auth = await loadSource('lib/authSession.ts');
const { useCartStore: store } = await loadSource('store/useCartStore.ts');
const item = { skuId: 10, productId: 1, shopId: 2, shopName: 'Shop', productName: 'Product', skuCode: 'SKU10', attributes: {}, imageUrl: '', price: 1, stockQuantity: 20 };
const user = { id: 1, fullName: 'Buyer', email: 'a@test.com', role: 'ROLE_CUSTOMER', status: 'ACTIVE' };
const session = { user, accessToken: 'access', refreshToken: 'refresh', rememberMe: false };
function dto() {
  return { userId: auth.readAuthSession()?.user.id, shops: serverItems.length ? [{ shopId: 2, shopName: 'Shop', items: serverItems, shopTotal: 0, shopItemCount: 0 }] : [], totalItems: serverItems.length, totalQuantity: 0, totalAmount: 0 };
}
function ok(config) { return { config, data: { data: dto() }, headers: {}, status: 200, statusText: 'OK' }; }
function normalTransport(config) {
  const payload = config.data ? JSON.parse(config.data) : {};
  if (config.method === 'post') {
    const old = serverItems.find(entry => entry.skuId === payload.skuId);
    if (old) old.quantity += payload.quantity;
    else serverItems.push({ ...item, productTitle: 'Server product', skuCode: 'REAL-SKU', skuVariant: '{"Color":"Blue"}', price: 100, stockAvailable: 20, quantity: payload.quantity });
  }
  if (config.method === 'put') {
    const target = serverItems.find(entry => entry.skuId === Number(config.url.split('/').at(-1)));
    target.quantity = payload.quantity;
  }
  if (config.method === 'delete') serverItems = payload.skuIds ? serverItems.filter(entry => !payload.skuIds.includes(entry.skuId)) : [];
  return Promise.resolve(ok(config));
}
test.beforeEach(async () => {
  auth.writeAuthSession(null);
  await store.getState().fetchCart();
  localStorage.clear(); sessionStorage.clear(); serverItems = []; transport = normalTransport;
  store.setState({ items: [], selectedSkuIds: [], error: null });
});

test('signed-in add/update/remove use authoritative server values and survive reload', async () => {
  auth.writeAuthSession(session);
  await store.getState().fetchCart();
  assert.equal(await store.getState().addToCart(item, 2), true);
  assert.equal(store.getState().items[0].price, 100);
  assert.deepEqual(store.getState().items[0].attributes, { Color: 'Blue' });
  assert.equal(store.getState().getTotalBadgeCount(), 2);
  assert.equal(localStorage.getItem('helishop-guest-cart-v3'), null);
  store.setState({ items: [], selectedSkuIds: [] });
  await store.getState().fetchCart();
  assert.equal(store.getState().items[0].quantity, 2);
  await store.getState().updateQuantity(10, 4);
  assert.equal(serverItems[0].quantity, 4);
  await store.getState().removeSelected();
  assert.equal(serverItems.length, 0);
  assert.equal(store.getState().items.length, 0);
});

test('guest merge uses maximum quantity so retries do not double existing server quantities', async () => {
  await store.getState().addToCart(item, 2);
  serverItems = [{ ...item, productTitle: 'Server', price: 100, stockAvailable: 20, skuVariant: '{}', quantity: 3 }];
  auth.writeAuthSession(session);
  await store.getState().fetchCart();
  assert.equal(serverItems[0].quantity, 3);
  assert.deepEqual(JSON.parse(localStorage.getItem('helishop-guest-cart-v3')), []);
});

test('failed mutation keeps confirmed items and reports failure instead of a false success', async () => {
  auth.writeAuthSession(session);
  await store.getState().fetchCart();
  transport = config => Promise.reject(new AxiosError('Out of stock', 'ERR_BAD_REQUEST', config, null, { status: 409, data: { message: 'Hết hàng' }, headers: {}, config, statusText: 'Conflict' }));
  assert.equal(await store.getState().addToCart(item, 2), false);
  assert.equal(store.getState().items.length, 0);
  assert.equal(store.getState().error, 'Hết hàng');
});

test('switching accounts clears old cart immediately and ignores the old response', async () => {
  auth.writeAuthSession(session);
  await store.getState().fetchCart();
  let finish;
  transport = config => new Promise(resolve => { finish = () => resolve({ ...ok(config), data: { data: { ...dto(), shops: [{ shopId: 2, shopName: 'Shop', items: [{ ...item, productTitle: 'Old account', quantity: 1, stockAvailable: 20 }] }] } } }); });
  const pending = store.getState().addToCart(item, 1);
  await new Promise(resolve => setTimeout(resolve, 0));
  auth.writeAuthSession({ ...session, user: { ...user, id: 2 }, refreshToken: 'refresh-2' });
  assert.equal(store.getState().items.length, 0);
  transport = normalTransport;
  finish();
  assert.equal(await pending, false);
  await store.getState().fetchCart();
  assert.equal(store.getState().items.length, 0);
});

test('cart DTO mapping preserves zero stock and plain legacy variant strings', () => {
  const mapped = mapCart({ shops: [{ shopId: 2, shopName: 'Shop', items: [{ ...item, productTitle: 'Sold out', quantity: 1, stockAvailable: 0, skuVariant: 'REAL-SKU' }] }] });
  assert.equal(mapped[0].stockQuantity, 0);
  assert.deepEqual(mapped[0].attributes, {});
});
