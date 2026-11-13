import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import ts from 'typescript';
import axios, { AxiosError } from 'axios';

const compile = source => ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 } }).outputText;
const asModule = source => `data:text/javascript;base64,${Buffer.from(compile(source)).toString('base64')}`;
const sessionUrl = asModule(await readFile(new URL('../src/lib/authSession.ts', import.meta.url), 'utf8'));
const auth = await import(sessionUrl);
const memoryStorage = () => {
  const values = new Map();
  return { getItem: key => values.get(key) ?? null, setItem: (key, value) => values.set(key, value), removeItem: key => values.delete(key), clear: () => values.clear() };
};
globalThis.localStorage = memoryStorage();
globalThis.sessionStorage = memoryStorage();
globalThis.window = { location: { href: '' } };
let transport;
axios.defaults.adapter = config => transport(config);
const apiSource = (await readFile(new URL('../src/lib/api.ts', import.meta.url), 'utf8'))
  .replace('from "axios"', `from ${JSON.stringify(import.meta.resolve('axios'))}`)
  .replace('from "@/lib/authSession"', `from ${JSON.stringify(sessionUrl)}`)
  .replace('import.meta.env.VITE_API_BASE_URL', 'undefined');
const { api } = await import(asModule(apiSource));
const user = { id: 1, fullName: 'Buyer', email: 'buyer@test.com', role: 'ROLE_CUSTOMER', status: 'ACTIVE' };
const session = { user, accessToken: 'expired', refreshToken: 'refresh-A', rememberMe: false };
const response = (config, data) => ({ config, data, status: 200, statusText: 'OK', headers: {} });
const unauthorized = config => Promise.reject(new AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, null, { ...response(config, {}), status: 401 }));
test.beforeEach(() => { auth.writeAuthSession(session); window.location.href = ''; });

test('parallel 401 responses perform one refresh and retry with the new token', async () => {
  let refreshes = 0;
  transport = async config => {
    if (config.url === '/auth/refresh-token') {
      refreshes++;
      await new Promise(resolve => setTimeout(resolve, 10));
      return response(config, { data: { accessToken: 'fresh' } });
    }
    return config.headers.Authorization === 'Bearer fresh' ? response(config, { ok: true }) : unauthorized(config);
  };
  const results = await Promise.all(Array.from({ length: 8 }, () => api.get('/cart')));
  assert.equal(refreshes, 1);
  assert.ok(results.every(result => result.data.ok));
  assert.equal(auth.readAuthSession().accessToken, 'fresh');
});

test('login 401 is returned to the form without refreshing or clearing another session', async () => {
  const urls = [];
  transport = config => { urls.push(config.url); return unauthorized(config); };
  await assert.rejects(api.post('/auth/login', {}));
  assert.deepEqual(urls, ['/auth/login']);
  assert.equal(auth.readAuthSession().refreshToken, 'refresh-A');
  assert.equal(window.location.href, '');
});

test('a retry that is still unauthorized stops after a single refresh', async () => {
  let refreshes = 0;
  transport = config => {
    if (config.url === '/auth/refresh-token') { refreshes++; return Promise.resolve(response(config, { data: { accessToken: 'fresh' } })); }
    return unauthorized(config);
  };
  await assert.rejects(api.get('/cart'));
  assert.equal(refreshes, 1);
});

test('refresh failure clears the matching session and signals login expiry', async () => {
  transport = config => unauthorized(config);
  await assert.rejects(api.get('/cart'));
  assert.equal(auth.readAuthSession(), null);
  assert.equal(window.location.href, '/login?expired=true');
});

test('an in-flight refresh cannot overwrite or log out a newly logged-in account', async () => {
  transport = config => {
    if (config.url === '/auth/refresh-token') {
      auth.writeAuthSession({ ...session, user: { ...user, id: 2 }, refreshToken: 'refresh-B', accessToken: 'B' });
      return Promise.resolve(response(config, { data: { accessToken: 'fresh-A' } }));
    }
    return unauthorized(config);
  };
  await assert.rejects(api.get('/cart'));
  assert.equal(auth.readAuthSession().user.id, 2);
  assert.equal(auth.readAuthSession().accessToken, 'B');
  assert.equal(window.location.href, '');
});
