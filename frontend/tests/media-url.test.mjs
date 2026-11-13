import test from 'node:test';
import assert from 'node:assert/strict';
import { loadSource } from './load-source.mjs';
const { mediaUrl } = await loadSource('lib/mediaUrl.ts');
test('uploaded media works behind the same-origin proxy and a remote API', () => {
  assert.equal(mediaUrl('/uploads/avatars/u.png'), '/uploads/avatars/u.png');
  assert.equal(mediaUrl('/uploads/avatars/u.png', 'https://api.example.test/api/v1'), 'https://api.example.test/uploads/avatars/u.png');
  assert.equal(mediaUrl('https://cdn.example.test/photo.png'), 'https://cdn.example.test/photo.png');
  assert.equal(mediaUrl(), '');
});
