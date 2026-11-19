import test from 'node:test';
import assert from 'node:assert/strict';
import axios, { AxiosError } from 'axios';
import { loadSource } from './load-source.mjs';
const values = new Map();
globalThis.sessionStorage = { getItem: key => values.get(key) ?? null, setItem: (key,value) => values.set(key,value), removeItem: key => values.delete(key) };
globalThis.localStorage = { getItem: () => null, setItem: () => {}, removeItem: () => {} };
let transport;
axios.defaults.adapter = config => transport(config);
const auth = await loadSource('lib/authSession.ts');
const { adminAccountService: service } = await loadSource('services/adminAccountService.ts');
const session = { user:{id:2,role:'ROLE_ADMIN'},accessToken:'access',refreshToken:'refresh',rememberMe:false };
const response = (config,data) => ({config,status:200,statusText:'OK',headers:{},data:{data}});
test.beforeEach(() => auth.writeAuthSession(session));
test('admin list sends only supported filters and authenticated session', async () => {
  transport = async config => {
    assert.equal(config.url,'/admin/users'); assert.equal(config.headers.Authorization,'Bearer access');
    assert.deepEqual(config.params,{page:2,size:20,keyword:'buyer',role:'ROLE_CUSTOMER',status:'BLOCKED'});
    return response(config,{items:[],page:2});
  };
  assert.deepEqual(await service.list({page:2,keyword:'buyer',role:'ROLE_CUSTOMER',status:'BLOCKED',actorId:999}),{items:[],page:2});
});
test('status update carries compare-and-set states and returns server account', async () => {
  transport = async config => {
    assert.equal(config.url,'/admin/users/8/status'); assert.equal(config.method,'patch');
    assert.deepEqual(JSON.parse(config.data),{expectedStatus:'ACTIVE',status:'BLOCKED'});
    return response(config,{id:8,status:'BLOCKED'});
  };
  assert.deepEqual(await service.update(8,'ACTIVE','BLOCKED'),{id:8,status:'BLOCKED'});
});
test('conflict and forbidden status updates surface errors', async () => {
  for (const status of [403,409]) {
    transport = async config => { throw new AxiosError('Rejected','ERR_BAD_REQUEST',config,null,{config,status,data:{message:'Changed'},headers:{},statusText:'Rejected'}); };
    await assert.rejects(service.update(8,'ACTIVE','BLOCKED'),error => error.response.status === status);
  }
});
test('queued admin mutation cannot use another account credentials', async () => {
  let sent=0; transport = async config => { sent++; return response(config,{}); };
  const pending = service.update(8,'ACTIVE','BLOCKED');
  auth.writeAuthSession({...session,user:{id:99,role:'ROLE_CUSTOMER'},refreshToken:'other'});
  await assert.rejects(pending); assert.equal(sent,0);
});
