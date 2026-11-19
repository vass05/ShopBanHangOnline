import { test, expect } from '@playwright/test';
import { randomUUID, randomInt } from 'node:crypto';
import { register, login, data, authorization, addAddress, checkout, signedPayment } from './helpers.mjs';

async function fixture(request, method = 'COD') {
  const seller = await register(request,'fulfillment-seller','ROLE_SELLER');
  await data(await request.post('/api/v1/shops/me',{headers:authorization(seller),data:{shopName:`Fulfillment-${randomUUID()}`}}),201);
  const categories = await data(await request.get('/api/v1/categories/tree'));
  const product = await data(await request.post('/api/v1/seller/products',{headers:authorization(seller),data:{
    product:{name:`Fulfillment product-${randomUUID()}`,categoryId:categories[0].id,status:'ACTIVE'},
    skus:[{skuCode:`FULFILL-${randomUUID()}`,price:100000,stockQuantity:10,attributes:{Color:'Red'}}],
  }}),201);
  const buyer = await register(request,'fulfillment-buyer');
  const address = await addAddress(request,buyer);
  const {orders} = await checkout(request,buyer,address,method,[{skuId:product.skus[0].id,quantity:2}]);
  return {seller,buyer,address,product,sku:product.skus[0],order:orders[0]};
}
const change = (request,seller,order,expectedStatus,status) => request.patch(`/api/v1/seller/orders/${order.id}/status`,{headers:authorization(seller),data:{expectedStatus,status}});
async function clickAction(page,label,status) {
  page.once('dialog',dialog => dialog.accept());
  await page.getByRole('button',{name:label,exact:true}).click();
  await expect(page.getByTestId('seller-order-status')).toHaveText(status);
}
async function actualStock(request,productId) { return (await data(await request.get(`/api/v1/products/${productId}`))).stockQuantity; }
const callback = order => signedPayment(order,{vnp_TransactionNo:String(Date.now())+String(randomInt(10000,99999))});

test('seller confirms, packs and hands over only own COD order; buyer sees saved states without invented payment', async ({page,request,browser,baseURL},testInfo) => {
  const f = await fixture(request); const foreign = await fixture(request);
  await login(page,f.seller);
  await page.getByRole('link',{name:'Xử lý đơn bán',exact:true}).click();
  await expect(page.getByRole('cell',{name:f.order.orderCode,exact:true})).toBeVisible();
  await expect(page.getByRole('cell',{name:foreign.order.orderCode,exact:true})).toHaveCount(0);
  await page.getByLabel('Tìm mã đơn bán',{exact:true}).fill(f.order.orderCode);
  await page.getByRole('button',{name:'Tìm đơn',exact:true}).click();
  await page.getByRole('link',{name:`Xử lý #${f.order.id}`,exact:true}).click();
  await expect(page.getByText(f.product.name,{exact:true})).toBeVisible();
  await expect(page.getByText(/0901234567/).first()).toBeVisible();
  await expect(page.getByText('Tổng: 230.000 ₫',{exact:true})).toBeVisible();
  const buyerContext = await browser.newContext({baseURL});
  try {
    const buyerPage = await buyerContext.newPage(); await login(buyerPage,f.buyer);
    await buyerPage.goto('/orders');
    const article = buyerPage.locator('article').filter({hasText:f.order.orderCode});
    await expect(article).toContainText('Chờ xác nhận');
    await clickAction(page,'Xác nhận đơn','Đã xác nhận');
    await buyerPage.reload(); await expect(article).toContainText('Đã xác nhận');
    await clickAction(page,'Bắt đầu đóng gói','Đang đóng gói');
    await page.reload(); await expect(page.getByTestId('seller-order-status')).toHaveText('Đang đóng gói');
    await page.evaluate(() => window.scrollTo(0,0));
    await page.screenshot({path:testInfo.outputPath('seller-order-processing.png'),fullPage:true});
    await clickAction(page,'Bàn giao vận chuyển','Đang giao');
    await buyerPage.reload(); await expect(article).toContainText('Đang giao');
    await expect(article.getByRole('button',{name:'Hủy đơn',exact:true})).toHaveCount(0);
    await clickAction(page,'Đánh dấu đã giao','Đã giao');
    await expect(page.getByTestId('seller-payment-status')).toHaveText('Chưa thanh toán · COD');
    await expect(page.getByText('Đã ghi nhận giao hàng; thanh toán COD chưa được đối soát',{exact:true})).toBeVisible();
    await page.reload(); await expect(page.getByTestId('seller-order-status')).toHaveText('Đã giao');
    await buyerPage.reload(); await expect(article).toContainText('Đã giao');
    expect(await actualStock(request,f.product.id)).toBe(8);
  } finally {await buyerContext.close();}
});

test('VNPAY payment does not pack the order; live intent and paid order cannot be cancelled; stale tab must reload', async ({page,request}) => {
  const f = await fixture(request,'VNPAY');
  await data(await request.post('/api/v1/payments/create-vnpay-url',{headers:authorization(f.buyer),data:{orderCode:f.order.orderCode}}));
  await login(page,f.seller); await page.goto(`/seller/orders?order=${f.order.id}`);
  await expect(page.getByText(/Đơn VNPAY chưa được xác nhận thanh toán/)).toBeVisible();
  await expect(page.getByRole('button',{name:'Hủy đơn',exact:true})).toHaveCount(0);
  await clickAction(page,'Xác nhận đơn','Đã xác nhận');
  await expect(page.getByRole('button',{name:'Bắt đầu đóng gói',exact:true})).toHaveCount(0);
  expect((await change(request,f.seller,f.order,'CONFIRMED','PROCESSING')).status()).toBe(400);
  const paid = await (await request.get('/api/v1/payments/vnpay-ipn',{params:callback(f.order)})).json();
  expect(paid.RspCode).toBe('00');
  const afterPayment = await data(await request.get(`/api/v1/seller/orders/${f.order.id}`,{headers:authorization(f.seller)}));
  expect(afterPayment.order.orderStatus).toBe('CONFIRMED'); expect(afterPayment.order.paymentStatus).toBe('PAID');
  expect((await change(request,f.seller,f.order,'CONFIRMED','CANCELLED')).status()).toBe(400);
  await page.getByRole('button',{name:'Tải lại dữ liệu',exact:true}).click();
  await expect(page.getByRole('button',{name:'Bắt đầu đóng gói',exact:true})).toBeVisible();
  await expect(page.getByRole('button',{name:'Hủy đơn',exact:true})).toHaveCount(0);
  await data(await change(request,f.seller,f.order,'CONFIRMED','PROCESSING'));
  await data(await change(request,f.seller,f.order,'PROCESSING','SHIPPING'));
  page.once('dialog',dialog => dialog.accept());
  await page.getByRole('button',{name:'Bắt đầu đóng gói',exact:true}).click();
  await expect(page.getByRole('alert')).toContainText('Đơn đã thay đổi');
  await page.getByRole('button',{name:'Tải lại dữ liệu',exact:true}).click();
  await expect(page.getByTestId('seller-order-status')).toHaveText('Đang giao');
  await clickAction(page,'Đánh dấu đã giao','Đã giao');
  await expect(page.getByTestId('seller-payment-status')).toHaveText('Đã thanh toán · VNPAY');
  expect(await actualStock(request,f.product.id)).toBe(8);
});

test('seller cancellation restores cached stock once, list retries failures and customer/other shop cannot act', async ({page,request}) => {
  const f = await fixture(request); const foreign = await fixture(request);
  expect(await actualStock(request,f.product.id)).toBe(8);
  const repeated = await Promise.all([change(request,f.seller,f.order,'PENDING','CANCELLED'),change(request,f.seller,f.order,'PENDING','CANCELLED')]);
  expect(repeated.map(response => response.status())).toEqual([200,200]);
  expect(await actualStock(request,f.product.id)).toBe(10);
  const history = await data(await request.get('/api/v1/orders/me',{headers:authorization(f.buyer)}));
  expect(history[0].orderStatus).toBe('CANCELLED');
  expect((await request.get(`/api/v1/seller/orders/${f.order.id}`,{headers:authorization(foreign.seller)})).status()).toBe(403);
  expect((await change(request,foreign.seller,f.order,'CANCELLED','CONFIRMED')).status()).toBe(403);
  expect((await request.get('/api/v1/seller/orders',{headers:authorization(f.buyer)})).status()).toBe(403);
  await login(page,f.seller);
  const pattern = /\/api\/v1\/seller\/orders\?/;
  await page.route(pattern,route => route.fulfill({status:503,contentType:'application/json',body:JSON.stringify({message:'Tạm thời chưa tải được đơn bán'})}));
  await page.goto('/seller/orders');
  await expect(page.getByRole('alert')).toContainText('Tạm thời chưa tải được đơn bán');
  await page.unroute(pattern); await page.getByRole('button',{name:'Tải lại danh sách',exact:true}).click();
  await page.getByLabel('Lọc trạng thái đơn bán',{exact:true}).selectOption('CANCELLED');
  await page.getByLabel('Lọc thanh toán đơn bán',{exact:true}).selectOption('UNPAID');
  await expect(page.getByRole('cell',{name:f.order.orderCode,exact:true})).toBeVisible();
  await page.getByRole('link',{name:`Xử lý #${f.order.id}`,exact:true}).click();
  await expect(page.getByTestId('seller-order-status')).toHaveText('Đã hủy');
  await expect(page.getByRole('button',{name:'Xác nhận đơn',exact:true})).toHaveCount(0);
  await page.goto('/profile'); await page.getByRole('button',{name:'Đăng xuất',exact:true}).click();
  await login(page,f.buyer); await page.goto('/seller/orders');
  await expect(page.getByRole('alert')).toContainText('Chức năng dành cho tài khoản người bán');
  await expect(page.getByRole('button',{name:'Xác nhận đơn',exact:true})).toHaveCount(0);
});

test('MySQL status races have one winner and late IPN is recorded without reopening a cancelled order', async ({request}) => {
  const f = await fixture(request);
  const result = await Promise.all([change(request,f.seller,f.order,'PENDING','CONFIRMED'),change(request,f.seller,f.order,'PENDING','CANCELLED')]);
  expect(result.map(response => response.status()).sort()).toEqual([200,409]);
  const stored = await data(await request.get(`/api/v1/seller/orders/${f.order.id}`,{headers:authorization(f.seller)}));
  expect(await actualStock(request,f.product.id)).toBe(stored.order.orderStatus === 'CANCELLED' ? 10 : 8);
  const paid = await fixture(request,'VNPAY');
  const [cancel,ipn] = await Promise.all([change(request,paid.seller,paid.order,'PENDING','CANCELLED'),request.get('/api/v1/payments/vnpay-ipn',{params:callback(paid.order)})]);
  expect([200,400]).toContain(cancel.status()); expect((await ipn.json()).RspCode).toBe('00');
  const final = await data(await request.get(`/api/v1/seller/orders/${paid.order.id}`,{headers:authorization(paid.seller)}));
  expect(final.order.paymentStatus).toBe('PAID');
  expect(['PENDING','CANCELLED']).toContain(final.order.orderStatus);
  if(final.order.orderStatus === 'CANCELLED') {
    expect(final.allowedStatuses).toEqual([]); expect(final.actionNote).toContain('đối soát/hoàn tiền riêng');
  } else {expect(final.allowedStatuses).toEqual(['CONFIRMED']);}
  expect(await actualStock(request,paid.product.id)).toBe(final.order.orderStatus === 'CANCELLED' ? 10 : 8);
});
