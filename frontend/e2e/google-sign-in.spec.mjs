import {test,expect} from '@playwright/test';
import {register,browserUser,authorization} from './helpers.mjs';

async function stubGoogle(page, result) {
  await page.route('https://accounts.google.com/gsi/client',route=>route.fulfill({contentType:'application/javascript',body:`
    window.google={accounts:{id:{initialize(options){window.testGoogleOptions=options;},renderButton(element){
      const button=document.createElement('button');button.textContent='Tiếp tục với Google';button.type='button';
      button.onclick=()=>window.testGoogleOptions.callback({credential:'test-provider-credential'});element.appendChild(button);
    }}}};
  `}));
  await page.route('**/api/v1/auth/google/config',route=>route.fulfill({json:{data:{enabled:true,clientId:'test-client.apps.googleusercontent.com'}}}));
  await page.route('**/api/v1/auth/google/challenge',route=>route.fulfill({json:{data:{nonce:'a'.repeat(64)}}}));
  await page.route('**/api/v1/auth/google',async route=>{
    expect(route.request().postDataJSON()).toEqual({credential:'test-provider-credential'});
    await route.fulfill({json:{data:result}});
  });
}

test('Google primary signup verifies first, chooses seller role and starts seller onboarding',async({page,request})=>{
  const seller=await register(request,'google-ui-seller','ROLE_SELLER');
  await stubGoogle(page,{status:'NEW_ACCOUNT',ticket:'ticket',email:seller.email,fullName:'Google Seller'});
  await page.route('**/api/v1/auth/google/complete',async route=>{
    expect(route.request().postDataJSON()).toEqual({ticket:'ticket',role:'ROLE_SELLER'});
    await route.fulfill({json:{data:{status:'AUTHENTICATED',auth:seller}}});
  });
  await page.goto('/register');
  await expect(page.locator('input[autocomplete="one-time-code"]')).not.toBeVisible();
  await page.getByRole('button',{name:'Tiếp tục với Google',exact:true}).click();
  await expect(page.getByText('Gmail đã được xác minh.',{exact:false})).toBeVisible();
  expect(await browserUser(page)).toBeNull();
  await page.getByLabel('Vai trò tài khoản',{exact:true}).selectOption('ROLE_SELLER');
  await page.getByRole('button',{name:'Hoàn tất đăng ký Google',exact:true}).click();
  await expect(page).toHaveURL(/\/seller\/shop$/);
  expect((await browserUser(page)).user.id).toBe(seller.userId);
  expect((await request.get('/api/v1/shops/me',{headers:authorization(seller)})).status()).toBe(200);
});

test('Google links an existing account only after password confirmation and preserves buyer role',async({page,request})=>{
  const buyer=await register(request,'google-ui-link');
  await stubGoogle(page,{status:'LINK_REQUIRED',ticket:'ticket',email:buyer.email,fullName:'Existing'});
  await page.route('**/api/v1/auth/google/complete',async route=>{
    expect(route.request().postDataJSON()).toEqual({ticket:'ticket',password:'ExistingPassword42!'});
    await route.fulfill({json:{data:{status:'AUTHENTICATED',auth:buyer}}});
  });
  await page.goto('/login'); await page.getByRole('button',{name:'Tiếp tục với Google',exact:true}).click();
  await expect(page.getByLabel('Mật khẩu HeliShop hiện tại')).toBeVisible(); expect(await browserUser(page)).toBeNull();
  await page.getByLabel('Mật khẩu HeliShop hiện tại').fill('ExistingPassword42!');
  await page.getByRole('button',{name:'Liên kết và đăng nhập',exact:true}).click();
  await expect(page).toHaveURL(/\/$/); expect((await browserUser(page)).user.role).toBe('ROLE_CUSTOMER');
});

test('disabled provider leaves legacy password login available and no fake Google popup',async({page})=>{
  await page.route('**/api/v1/auth/google/config',route=>route.fulfill({json:{data:{enabled:false}}}));
  await page.goto('/login');
  await expect(page.getByRole('button',{name:'Tiếp tục với Google',exact:true})).toBeDisabled();
  await expect(page.getByRole('button',{name:'Đăng Nhập',exact:true})).toBeVisible();
  await expect(page.getByRole('alert')).toContainText('Google tạm chưa khả dụng');
});

test('expired Google onboarding never signs in and exposes a retry',async({page})=>{
  await stubGoogle(page,{status:'NEW_ACCOUNT',ticket:'ticket',email:'owner@gmail.com',fullName:'Owner'});
  await page.route('**/api/v1/auth/google/complete',route=>route.fulfill({status:401,json:{message:'Phiên xác thực Google đã hết hạn'}}));
  await page.goto('/register'); await page.getByRole('button',{name:'Tiếp tục với Google',exact:true}).click();
  await page.getByRole('button',{name:'Hoàn tất đăng ký Google',exact:true}).click();
  await expect(page.getByRole('alert')).toContainText('đã hết hạn'); expect(await browserUser(page)).toBeNull();
  await expect(page.getByRole('button',{name:'Thử lại Google',exact:true})).toBeVisible();
});

test('a delayed Google result cannot restore a session after another sign-in and logout',async({page,request})=>{
  const user=await register(request,'google-ui-stale');
  await stubGoogle(page,{status:'AUTHENTICATED',auth:user});
  let release, arrived;
  const gate=new Promise(resolve=>{release=resolve;});
  const posted=new Promise(resolve=>{arrived=resolve;});
  await page.route('**/api/v1/auth/google',async route=>{arrived();await gate;await route.fulfill({json:{data:{status:'AUTHENTICATED',auth:user}}});});
  await page.goto('/login');await page.getByRole('button',{name:'Tiếp tục với Google',exact:true}).click();await posted;
  await page.evaluate(user=>{
    const key='helishop-session-v3';
    sessionStorage.setItem(key,JSON.stringify({user:{id:user.userId,role:user.role,email:user.email},accessToken:user.accessToken,refreshToken:user.refreshToken,rememberMe:false}));
    window.dispatchEvent(new StorageEvent('storage',{key}));
    sessionStorage.removeItem(key);window.dispatchEvent(new StorageEvent('storage',{key}));
  },user);
  release();
  await expect(page.getByRole('alert')).toContainText('Phiên đăng nhập đã thay đổi');
  expect(await browserUser(page)).toBeNull();
  await page.getByRole('button',{name:'Thử lại Google',exact:true}).click();
  await expect(page.getByRole('button',{name:'Tiếp tục với Google',exact:true})).toBeVisible();
});
