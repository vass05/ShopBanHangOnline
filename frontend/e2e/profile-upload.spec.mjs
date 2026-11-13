import { test, expect } from '@playwright/test';
import { execFileSync } from 'node:child_process';
import { register, login, authorization, browserUser } from './helpers.mjs';

test('profile avatar renders through Nginx and survives recreating only the backend', async ({ page, request }) => {
  test.setTimeout(180_000);
  const user = await register(request, 'avatar');
  await login(page, user);
  await page.goto('/profile');
  // Generate a small valid PNG in the browser rather than depending on an external image URL.
  const png = Buffer.from(await page.evaluate(() => {
    const canvas = document.createElement('canvas');
    canvas.width = canvas.height = 2;
    canvas.getContext('2d').fillRect(0, 0, 2, 2);
    return canvas.toDataURL('image/png').split(',')[1];
  }), 'base64');
  const uploaded = page.waitForResponse(response => response.url().endsWith('/users/me/avatar'));
  await page.locator('input[type="file"]').setInputFiles({ name: 'avatar.png', mimeType: 'image/png', buffer: png });
  const response = await uploaded;
  expect(response.status()).toBe(200);
  const avatar = (await response.json()).data.avatarUrl;
  expect(avatar).toMatch(/^\/uploads\/avatars\/user_\d+_[\da-f-]+\.png$/);
  const image = page.locator(`img[src="${avatar}"]`).first();
  await expect.poll(() => image.evaluate(node => node.complete && node.naturalWidth > 0)).toBeTruthy();
  const session = await browserUser(page);
  const forged = await request.post('/api/v1/users/me/avatar', { headers: authorization(session), multipart: { file: { name: 'bad.png', mimeType: 'image/png', buffer: Buffer.from('<script>bad</script>') } } });
  expect(forged.status()).toBe(400);
  const oversized = await request.post('/api/v1/users/me/avatar', { headers: authorization(session), multipart: { file: { name: 'large.png', mimeType: 'image/png', buffer: Buffer.alloc(5*1024*1024+1) } } });
  expect(oversized.status()).toBe(413);
  const label = execFileSync('docker', ['inspect', '--format', '{{ index .Config.Labels "com.docker.compose.project" }}', 'helishop_integration_backend'], { encoding: 'utf8' }).trim();
  expect(label).toBe('helishop-integration');
  execFileSync('docker', ['compose','--env-file','.env.integration.local','-f','docker-compose.production.yml','-f','docker-compose.e2e.yml',
    'up','-d','--force-recreate','--no-deps','--wait','--wait-timeout','150','backend'], { cwd: new URL('../../', import.meta.url), timeout: 160_000, stdio: ['ignore','pipe','pipe'] });
  await expect.poll(async () => {
    try { return (await request.get(avatar)).status(); } catch { return 0; }
  }, { timeout: 25_000 }).toBe(200);
  expect(await (await request.get(avatar)).body()).toEqual(png);
  await page.reload();
  await expect.poll(() => image.evaluate(node => node.complete && node.naturalWidth > 0)).toBeTruthy();
});
