import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import ts from 'typescript';

// Execute the actual dependency-free session module on Node 20+.
const source = await readFile(new URL('../src/lib/authSession.ts', import.meta.url), 'utf8');
const { outputText } = ts.transpileModule(source, { compilerOptions: { module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022 } });
const auth = await import(`data:text/javascript;base64,${Buffer.from(outputText).toString('base64')}`);
const memoryStorage = () => {
  const values = new Map();
  return { getItem: key => values.get(key) ?? null, setItem: (key, value) => values.set(key, value), removeItem: key => values.delete(key), clear: () => values.clear() };
};
globalThis.localStorage = memoryStorage();
globalThis.sessionStorage = memoryStorage();
const user = { id: 1, fullName: 'Buyer', email: 'buyer@test.com', role: 'ROLE_CUSTOMER', status: 'ACTIVE' };
const session = { user, accessToken: 'access', refreshToken: 'refresh', rememberMe: false };
test.beforeEach(() => { localStorage.clear(); sessionStorage.clear(); });

test('without remember-me, no credentials persist in localStorage, including refresh', () => {
  auth.writeAuthSession(session);
  auth.updateSessionAccessToken('new-access');
  assert.equal(localStorage.getItem('helishop-session-v3'), null);
  assert.equal(localStorage.getItem('helishop-auth-storage'), null);
  assert.equal(auth.readAuthSession().accessToken, 'new-access');
  sessionStorage.clear();
  assert.equal(auth.readAuthSession(), null);
});

test('remembered session survives tab storage clearing and is removed when switching to non-remembered account', () => {
  auth.writeAuthSession({ ...session, rememberMe: true });
  sessionStorage.clear();
  assert.equal(auth.readAuthSession().user.id, 1);
  auth.writeAuthSession({ ...session, user: { ...user, id: 2 } });
  assert.equal(localStorage.getItem('helishop-session-v3'), null);
  assert.equal(auth.readAuthSession().user.id, 2);
});

test('refresh and clear notify store subscribers with updated identity and token', () => {
  const updates = [];
  const unsubscribe = auth.subscribeAuthSession(value => updates.push(value));
  auth.writeAuthSession(session);
  auth.updateSessionAccessToken('refreshed');
  auth.writeAuthSession(null);
  unsubscribe();
  assert.equal(updates[1].accessToken, 'refreshed');
  assert.equal(updates[1].user.id, 1);
  assert.equal(updates[2], null);
  assert.equal(auth.readAuthSession(), null);
});

test('legacy Zustand metadata alone cannot resurrect a logged-out session', () => {
  localStorage.setItem('helishop-auth-storage', JSON.stringify({ state: session }));
  assert.equal(auth.readAuthSession(), null);
  assert.equal(localStorage.getItem('helishop-auth-storage'), null);
});

test('migrates a valid legacy tab session without copying credentials to persistent storage', () => {
  localStorage.setItem('helishop-auth-storage', JSON.stringify({ state: session }));
  sessionStorage.setItem('accessToken', 'access');
  sessionStorage.setItem('refreshToken', 'refresh');
  assert.equal(auth.readAuthSession().rememberMe, false);
  assert.equal(localStorage.getItem('helishop-auth-storage'), null);
  assert.equal(localStorage.getItem('helishop-session-v3'), null);
  assert.ok(sessionStorage.getItem('helishop-session-v3'));
});

test('storage synchronization notifies all identity subscribers after a cross-tab account change', () => {
  const updates = [];
  const unsubscribe = auth.subscribeAuthSession(value => updates.push(value));
  localStorage.setItem('helishop-session-v3', JSON.stringify({ ...session, user: { ...user, id: 2 }, rememberMe: true }));
  auth.synchronizeAuthSession();
  unsubscribe();
  assert.equal(updates[0].user.id, 2);
});
