import { test, expect } from '@playwright/test';
import { register, login, sql, data, authorization } from './helpers.mjs';

async function admin(request) {
  const user = await register(request,'admin-e2e');
  // Promote only the fresh test account, inside the guarded integration database.
  sql(`UPDATE users SET role='ROLE_ADMIN' WHERE id=${Number(user.userId)} AND email='${user.email}';`);
  return { ...await data(await request.post('/api/v1/auth/login',{data:{email:user.email,password:user.password}})),password:user.password };
}

test('admin filters accounts, blocks buyer immediately, unblocks with old sessions still revoked', async ({page,request}) => {
  const manager = await admin(request), buyer = await register(request,'admin-target');
  await login(page,manager);
  await page.getByLabel('Tìm email hoặc họ tên').fill(buyer.email);
  await page.getByRole('button',{name:'Tìm tài khoản',exact:true}).click();
  await expect(page.getByRole('cell',{name:buyer.email,exact:false})).toBeVisible();
  page.on('dialog',dialog => dialog.accept());
  await page.getByRole('button',{name:'Khóa tài khoản',exact:true}).click();
  await expect(page.getByRole('cell',{name:'Đã khóa',exact:true})).toBeVisible();
  expect((await request.get('/api/v1/users/me',{headers:authorization(buyer)})).status()).toBe(401);
  expect((await request.post('/api/v1/auth/refresh-token',{data:{refreshToken:buyer.refreshToken}})).status()).toBe(401);
  expect((await request.post('/api/v1/auth/login',{data:{email:buyer.email,password:buyer.password}})).status()).toBe(401);
  await page.getByRole('button',{name:'Mở khóa tài khoản',exact:true}).click();
  await expect(page.getByRole('cell',{name:'Hoạt động',exact:true})).toBeVisible();
  expect((await request.get('/api/v1/users/me',{headers:authorization(buyer)})).status()).toBe(401);
  const fresh = await data(await request.post('/api/v1/auth/login',{data:{email:buyer.email,password:buyer.password}}));
  expect((await request.get('/api/v1/users/me',{headers:authorization(fresh)})).status()).toBe(200);
  await page.getByLabel('Lọc vai trò tài khoản').selectOption('ROLE_SELLER');
  await expect(page.getByText('Không có tài khoản phù hợp.',{exact:true})).toBeVisible();
});

test('buyer and seller cannot use admin API or UI and stale account edits conflict', async ({page,request}) => {
  const manager = await admin(request), seller = await register(request,'admin-seller','ROLE_SELLER'), buyer = await register(request,'admin-denied');
  for (const user of [seller,buyer]) {
    expect((await request.get('/api/v1/admin/users',{headers:authorization(user)})).status()).toBe(403);
    expect((await request.patch(`/api/v1/admin/users/${buyer.userId}/status`,{headers:authorization(user),data:{expectedStatus:'ACTIVE',status:'BLOCKED'}})).status()).toBe(403);
  }
  await login(page,buyer); await page.goto('/admin/users');
  await expect(page.getByRole('alert')).toHaveText('Chức năng dành cho quản trị viên.');
  expect((await request.patch(`/api/v1/admin/users/${seller.userId}/status`,{headers:authorization(manager),data:{expectedStatus:'ACTIVE',status:'BLOCKED'}})).status()).toBe(200);
  expect((await request.get('/api/v1/seller/orders',{headers:authorization(seller)})).status()).toBe(401);
  expect((await request.patch(`/api/v1/admin/users/${seller.userId}/status`,{headers:authorization(manager),data:{expectedStatus:'INACTIVE',status:'ACTIVE'}})).status()).toBe(409);
});
