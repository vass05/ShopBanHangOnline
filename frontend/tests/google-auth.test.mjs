import test from 'node:test';
import assert from 'node:assert/strict';
import axios, { AxiosError } from 'axios';
import { loadSource } from './load-source.mjs';
const values=new Map();
globalThis.sessionStorage={getItem:key=>values.get(key)??null,setItem:(key,value)=>values.set(key,value),removeItem:key=>values.delete(key)};
globalThis.localStorage={getItem:()=>null,setItem:()=>{},removeItem:()=>{}};
let transport; axios.defaults.adapter=config=>transport(config);
const auth=await loadSource('lib/authSession.ts');
const {googleAuthService:service}=await loadSource('services/googleAuthService.ts');
const response=(config,data)=>({config,status:200,statusText:'OK',headers:{},data:{data}});
test.beforeEach(()=>auth.writeAuthSession(null));
test('Google runtime config and challenge use public endpoints with session guard',async()=>{
  transport=async config=>{
    if(config.url.endsWith('/config')) return response(config,{enabled:true,clientId:'test.apps.googleusercontent.com'});
    assert.equal(config.url,'/auth/google/challenge'); assert.deepEqual(JSON.parse(config.data),{});
    return response(config,{nonce:'nonce'});
  };
  assert.equal((await service.config()).clientId,'test.apps.googleusercontent.com');
  assert.deepEqual(await service.challenge(),{nonce:'nonce'});
});
test('Google login sends only credential and preserves pending state without creating a local session',async()=>{
  const pending={status:'NEW_ACCOUNT',ticket:'ticket',email:'owner@gmail.com'};
  transport=async config=>{assert.equal(config.url,'/auth/google');assert.deepEqual(JSON.parse(config.data),{credential:'signed-google-token'});return response(config,pending);};
  assert.deepEqual(await service.login('signed-google-token'),pending); assert.equal(auth.readAuthSession(),null);
});
test('completion strips client-supplied identity and only sends ticket, role or link password',async()=>{
  transport=async config=>{
    assert.equal(config.url,'/auth/google/complete');
    assert.deepEqual(JSON.parse(config.data),{ticket:'ticket',role:'ROLE_SELLER'});
    return response(config,{status:'AUTHENTICATED',auth:{userId:4,role:'ROLE_SELLER'}});
  };
  assert.equal((await service.complete('ticket',{role:'ROLE_SELLER',email:'attacker@gmail.com',userId:999})).auth.userId,4);
  transport=async config=>{assert.deepEqual(JSON.parse(config.data),{ticket:'ticket',password:'existing'});return response(config,{});};
  await service.complete('ticket',{password:'existing'});
});
test('expired and disabled provider errors propagate without fabricating credentials',async()=>{
  for(const status of [401,503]){
    transport=async config=>{throw new AxiosError('Rejected','ERR_BAD_REQUEST',config,null,{config,status,data:{message:'Unavailable'},headers:{},statusText:'Rejected'});};
    await assert.rejects(service.login('invalid'),error=>error.response.status===status);
    assert.equal(auth.readAuthSession(),null);
  }
});
test('queued Google completion cannot use a replacement session',async()=>{
  let sent=0; transport=async config=>{sent++;return response(config,{});};
  const pending=service.complete('ticket',{role:'ROLE_CUSTOMER'});
  auth.writeAuthSession({user:{id:5},accessToken:'other',refreshToken:'other',rememberMe:false});
  await assert.rejects(pending); assert.equal(sent,0);
});
