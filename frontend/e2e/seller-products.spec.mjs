import { test, expect } from '@playwright/test';
import { randomUUID } from 'node:crypto';
import { register, login, data, authorization, addAddress, checkout } from './helpers.mjs';

async function sellerWithShop(request) {
  const seller = await register(request, 'product-seller', 'ROLE_SELLER');
  await data(await request.post('/api/v1/shops/me', { headers: authorization(seller), data: { shopName: `Shop-${randomUUID()}` } }), 201);
  return seller;
}
async function fixture(request, seller, count = 1) {
  const categories = await data(await request.get('/api/v1/categories/tree'));
  const requestBody = { product: { name: `Seller-${randomUUID()}`, categoryId: categories[0].id, status: 'ACTIVE' }, skus: Array.from({ length: count }, (_, i) => ({
    skuCode: `SKU-${randomUUID()}`, price: 100000 + i * 10000, stockQuantity: 10, attributes: { Color: i ? 'Blue' : 'Red' },
  })) };
  return { product: await data(await request.post('/api/v1/seller/products', { headers: authorization(seller), data: requestBody }), 201), body: requestBody };
}
const skuPayload = (sku, overrides = {}) => ({ skuCode: sku.skuCode, price: sku.price, stockQuantity: sku.stockQuantity, attributes: JSON.parse(sku.skuAttributes), version: sku.version, ...overrides });

test('seller creates and edits product/SKUs; customer buys the new variant; stale inventory edit is rejected and hide removes public access', async ({ page, request, browser, baseURL }, testInfo) => {
  const seller = await sellerWithShop(request);
  await login(page, seller);
  await page.getByRole('link', { name: 'Quản lý sản phẩm & SKU', exact: true }).click();
  await page.getByRole('link', { name: 'Thêm sản phẩm', exact: true }).click();
  const name = `Áo seller-${randomUUID()}`;
  const redCode = `RED-${randomUUID()}`.toUpperCase();
  const blueCode = `BLUE-${randomUUID()}`.toUpperCase();
  await page.getByLabel('Tên sản phẩm', { exact: true }).fill(name);
  await page.getByLabel('Danh mục', { exact: true }).selectOption({ index: 1 });
  await page.getByLabel('Mô tả sản phẩm', { exact: true }).fill('Sản phẩm do người bán tạo, hai màu.');
  await page.getByLabel('Mã SKU', { exact: true }).fill(redCode);
  await page.getByLabel('Giá bán (đ)', { exact: true }).fill('100000');
  await page.getByLabel('Tồn kho SKU', { exact: true }).fill('5');
  await page.getByLabel('Tên thuộc tính 1', { exact: true }).fill('Màu sắc');
  await page.getByLabel('Giá trị thuộc tính 1', { exact: true }).fill('Đỏ');
  const created = page.waitForResponse(response => /\/api\/v1\/seller\/products$/.test(response.url()) && response.request().method() === 'POST');
  await page.getByRole('button', { name: 'Tạo sản phẩm', exact: true }).click();
  const product = await data(await created, 201);
  await expect(page).toHaveURL(new RegExp(`product=${product.id}$`));
  expect((await request.get(`/api/v1/products/${product.id}`)).status()).toBe(404);
  await page.getByLabel('Trạng thái sản phẩm', { exact: true }).selectOption('ACTIVE');
  await page.getByRole('button', { name: 'Lưu thông tin', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('Đã lưu thông tin sản phẩm');
  await data(await request.get(`/api/v1/products/${product.id}`)); // Warm Redis before the SKU edits.
  await page.getByLabel('Mã SKU', { exact: true }).fill(blueCode);
  await page.getByLabel('Giá bán (đ)', { exact: true }).fill('80000');
  await page.getByLabel('Tồn kho SKU', { exact: true }).fill('6');
  await page.getByLabel('Giá trị thuộc tính 1', { exact: true }).fill('Xanh');
  await page.getByRole('button', { name: 'Thêm SKU', exact: true }).click();
  await expect(page.getByTestId('seller-total-stock')).toHaveText('11');
  await page.getByRole('button', { name: `Sửa SKU ${redCode}`, exact: true }).click();
  await page.getByLabel('Giá bán (đ)', { exact: true }).fill('120000');
  await page.getByLabel('Tồn kho SKU', { exact: true }).fill('8');
  await page.getByRole('button', { name: 'Lưu SKU', exact: true }).click();
  await expect(page.getByTestId('seller-total-stock')).toHaveText('14');
  await page.reload();
  await expect(page.getByTestId('seller-total-stock')).toHaveText('14');
  const publicProduct = await data(await request.get(`/api/v1/products/${product.id}`));
  expect(Number(publicProduct.price)).toBe(80000); expect(publicProduct.stockQuantity).toBe(14);
  await page.screenshot({ path: testInfo.outputPath('seller-product-editor.png'), fullPage: true });
  const buyer = await register(request, 'seller-product-buyer');
  await addAddress(request, buyer);
  const buyerContext = await browser.newContext({ baseURL });
  try {
    const buyerPage = await buyerContext.newPage();
    await login(buyerPage, buyer);
    await buyerPage.goto(`/product/${product.id}`);
    await expect(buyerPage.getByRole('heading', { name, exact: true })).toBeVisible();
    await buyerPage.getByRole('button', { name: 'Xanh', exact: true }).click();
    await buyerPage.getByRole('button', { name: 'Thêm Vào Giỏ Hàng', exact: true }).click();
    await expect(buyerPage.getByText('Đã thêm vào giỏ hàng thành công!', { exact: true })).toBeVisible();
    await buyerPage.goto('/cart');
    await buyerPage.getByRole('checkbox').first().setChecked(true);
    await buyerPage.getByRole('button', { name: /Mua Hàng \(1\)/ }).click();
    await buyerPage.getByRole('button', { name: 'Đặt hàng', exact: true }).click();
    await expect(buyerPage.getByRole('heading', { name: 'Đã tạo 1 đơn hàng' })).toBeVisible();
    const orders = await data(await request.get('/api/v1/orders/me', { headers: authorization(buyer) }));
    expect(Number(orders[0].finalAmount)).toBe(110000);
    const sku = publicProduct.skus.find(item => item.skuCode === blueCode);
    await page.getByRole('button', { name: `Sửa SKU ${blueCode}`, exact: true }).click();
    await page.getByLabel('Tồn kho SKU', { exact: true }).fill('100');
    await page.getByRole('button', { name: 'Lưu SKU', exact: true }).click();
    await expect(page.getByRole('alert')).toContainText('SKU đã thay đổi');
    expect((await data(await request.get(`/api/v1/products/${product.id}`))).stockQuantity).toBe(13);
    await page.getByRole('button', { name: 'Tải lại dữ liệu', exact: true }).click();
    await expect(page.getByTestId('seller-total-stock')).toHaveText('13');
    await page.getByRole('button', { name: `Sửa SKU ${blueCode}`, exact: true }).click();
    await page.getByLabel('Tồn kho SKU', { exact: true }).fill('7');
    await page.getByRole('button', { name: 'Lưu SKU', exact: true }).click();
    await expect(page.getByTestId('seller-total-stock')).toHaveText('15');
    await page.getByLabel('Trạng thái sản phẩm', { exact: true }).selectOption('INACTIVE');
    await page.getByRole('button', { name: 'Lưu thông tin', exact: true }).click();
    await expect(page.getByRole('status')).toContainText('Đã lưu thông tin sản phẩm');
    expect((await request.get(`/api/v1/products/${product.id}`)).status()).toBe(404);
    expect((await request.post('/api/v1/orders/checkout', { headers: authorization(buyer), data: { paymentMethod: 'COD', shippingAddressSnapshot: 'Test', items: [{ skuId: sku.id, quantity: 1 }] } })).status()).toBe(400);
  } finally { await buyerContext.close(); }
  await page.getByRole('link', { name: '← Danh sách sản phẩm', exact: true }).click();
  await page.getByLabel('Lọc trạng thái', { exact: true }).selectOption('INACTIVE');
  await page.getByLabel('Tìm sản phẩm của tôi', { exact: true }).fill(name);
  await page.getByRole('button', { name: 'Tìm kiếm', exact: true }).last().click();
  await expect(page.getByRole('cell', { name, exact: true })).toBeVisible();
});

test('seller list retries server failures; duplicate SKU preserves input and cross-shop/customer access is denied', async ({ page, request }) => {
  const seller = await sellerWithShop(request);
  const { product } = await fixture(request, seller);
  const other = await sellerWithShop(request);
  expect((await request.get(`/api/v1/seller/products/${product.id}`, { headers: authorization(other) })).status()).toBe(403);
  expect((await request.put(`/api/v1/seller/products/${product.id}/skus/${product.skus[0].id}`, { headers: authorization(other), data: skuPayload(product.skus[0]) })).status()).toBe(403);
  await login(page, seller);
  const listPattern = /\/api\/v1\/seller\/products\?/;
  await page.route(listPattern, route => route.fulfill({ status: 503, contentType: 'application/json', body: JSON.stringify({ message: 'Tạm thời không tải được danh sách' }) }));
  await page.goto('/seller/products');
  await expect(page.getByRole('alert')).toContainText('Tạm thời không tải được danh sách');
  await page.unroute(listPattern);
  await page.getByRole('button', { name: 'Tải lại danh sách' }).click();
  await page.getByRole('link', { name: `Quản lý #${product.id}`, exact: true }).click();
  await page.getByLabel('Mã SKU', { exact: true }).fill(product.skus[0].skuCode.toLowerCase());
  await page.getByLabel('Giá bán (đ)', { exact: true }).fill('50000');
  await page.getByLabel('Giá trị thuộc tính 1', { exact: true }).fill('Blue');
  await page.getByRole('button', { name: 'Thêm SKU', exact: true }).click();
  await expect(page.getByRole('alert')).toContainText('Mã SKU đã được sử dụng');
  await expect(page.getByLabel('Giá trị thuộc tính 1', { exact: true })).toHaveValue('Blue');
  await page.getByLabel('Mã SKU', { exact: true }).fill(`UNIQUE-${randomUUID()}`);
  await page.getByRole('button', { name: 'Thêm SKU', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('Đã lưu SKU');
  const customer = await register(request, 'seller-access-customer');
  expect((await request.get('/api/v1/seller/products', { headers: authorization(customer) })).status()).toBe(403);
  await page.goto('/profile'); await page.getByRole('button', { name: 'Đăng xuất', exact: true }).click();
  await login(page, customer); await page.goto('/seller/products');
  await expect(page.getByRole('alert')).toContainText('Chức năng dành cho tài khoản người bán');
  await expect(page.getByRole('link', { name: 'Thêm sản phẩm', exact: true })).toHaveCount(0);
});

test('real MySQL serializes SKU edits with checkout and preserves aggregate across concurrent variants', async ({ request }) => {
  const seller = await sellerWithShop(request);
  const { product } = await fixture(request, seller);
  const buyer = await register(request, 'sku-race-buyer');
  const address = await addAddress(request, buyer);
  const sku = product.skus[0];
  const update = (productId, sku, overrides) => request.put(`/api/v1/seller/products/${productId}/skus/${sku.id}`, { headers: authorization(seller), data: skuPayload(sku, overrides) });
  const [edit] = await Promise.all([update(product.id, sku, { stockQuantity: 6 }), checkout(request, buyer, address, 'COD', [{ skuId: sku.id, quantity: 1 }])]);
  expect([200,409]).toContain(edit.status());
  const latest = await data(await request.get(`/api/v1/seller/products/${product.id}`, { headers: authorization(seller) }));
  expect(latest.stockQuantity).toBe(edit.status() === 200 ? 5 : 9);
  expect(latest.skus[0].stockQuantity).toBe(latest.stockQuantity);
  const repeated = await Promise.all([update(product.id, latest.skus[0], { stockQuantity: 8 }), update(product.id, latest.skus[0], { stockQuantity: 7 })]);
  expect(repeated.map(response => response.status()).sort()).toEqual([200,409]);
  const two = (await fixture(request, seller, 2)).product;
  const variants = await Promise.all([update(two.id,two.skus[0],{ stockQuantity: 3, price: 80000 }),update(two.id,two.skus[1],{ stockQuantity: 5, price: 70000 })]);
  expect(variants.map(response => response.status())).toEqual([200,200]);
  const publicProduct = await data(await request.get(`/api/v1/products/${two.id}`));
  expect(publicProduct.stockQuantity).toBe(8); expect(Number(publicProduct.price)).toBe(70000);
});
