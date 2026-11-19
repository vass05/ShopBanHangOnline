import test from 'node:test';
import assert from 'node:assert/strict';
import axios, { AxiosError } from 'axios';
import { loadSource } from './load-source.mjs';
const values = new Map();
globalThis.sessionStorage = { getItem: key => values.get(key) ?? null, setItem: (key, value) => values.set(key,value), removeItem: key => values.delete(key) };
globalThis.localStorage = { getItem: () => null, setItem: () => {}, removeItem: () => {} };
let transport;
axios.defaults.adapter = config => transport(config);
const auth = await loadSource('lib/authSession.ts');
const { sellerOrderService: service } = await loadSource('services/sellerOrderService.ts');
const session = { user: { id: 3, role: 'ROLE_SELLER' }, accessToken: 'access', refreshToken: 'refresh', rememberMe: false };
const response = (config,data) => ({ config,status:200,statusText:'OK',headers:{},data:{data} });
test.beforeEach(() => auth.writeAuthSession(session));

test('seller list preserves pagination/state/payment filters and never sends a client shop override', async () => {
  const page = { page:2,size:20,totalElements:21,totalPages:2,items:[] };
  transport = async config => {
    assert.equal(config.url,'/seller/orders'); assert.equal(config.headers.Authorization,'Bearer access');
    assert.deepEqual(config.params,{page:2,size:20,keyword:'ORD-',status:'CONFIRMED',paymentStatus:'PAID'});
    return response(config,page);
  };
  assert.deepEqual(await service.list({page:2,keyword:'ORD-',status:'CONFIRMED',paymentStatus:'PAID',shopId:999}),page);
});

test('detail keeps server-provided action availability and snapshot amounts without inferring payment', async () => {
  const detail = { order:{id:9,orderStatus:'CONFIRMED',paymentMethod:'VNPAY',paymentStatus:'UNPAID',finalAmount:230000},allowedStatuses:['CANCELLED'],actionNote:'Chờ thanh toán' };
  transport = async config => { assert.equal(config.url,'/seller/orders/9'); return response(config,detail); };
  assert.deepEqual(await service.detail(9),detail);
});

test('fulfillment sends compare-and-set statuses and returns the confirmed server response', async () => {
  const updated = { order:{id:9,orderStatus:'PROCESSING',paymentStatus:'PAID'},allowedStatuses:['SHIPPING'] };
  transport = async config => {
    assert.equal(config.url,'/seller/orders/9/status'); assert.equal(config.method,'patch');
    assert.deepEqual(JSON.parse(config.data),{expectedStatus:'CONFIRMED',status:'PROCESSING'});
    return response(config,updated);
  };
  assert.deepEqual(await service.transition(9,'CONFIRMED','PROCESSING'),updated);
});

test('stale status and payment-rule failures propagate without fabricating a successful state', async () => {
  for(const status of [400,409,403]) {
    transport = async config => { throw new AxiosError('Rejected','ERR_BAD_REQUEST',config,null,{config,status,data:{message:'Đơn đã thay đổi'},headers:{},statusText:'Rejected'}); };
    await assert.rejects(service.transition(9,'PENDING','CONFIRMED'),error => error.response.status === status);
  }
});

test('a queued order mutation cannot be sent using the next seller session', async () => {
  let sent = 0;
  transport = async config => { sent++; return response(config,{}); };
  const pending = service.transition(9,'PENDING','CONFIRMED');
  auth.writeAuthSession({...session,user:{id:99},refreshToken:'other'});
  await assert.rejects(pending); assert.equal(sent,0);
});
