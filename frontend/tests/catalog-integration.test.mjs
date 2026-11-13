import test from 'node:test';
import assert from 'node:assert/strict';
import axios from 'axios';
import { loadSource } from './load-source.mjs';
globalThis.localStorage = globalThis.sessionStorage = { getItem: () => null, removeItem: () => {}, setItem: () => {} };
let transport;
axios.defaults.adapter = config => transport(config);
const { mapProduct, mapCategory, catalogService } = await loadSource('services/catalogService.ts');
const product = { id: 1, name: 'Product', slug: 'product', price: 100, rating: null, stockQuantity: 0, shopId: 2, shopName: 'Shop', categoryId: 3, categoryName: 'Category' };

test('missing SKU, zero stock and unknown metrics never become fabricated sellable data', () => {
  const mapped = mapProduct(product);
  assert.equal(mapped.productSkus.length, 0); assert.equal(mapped.stockQuantity, 0);
  assert.equal(mapped.originalPrice, undefined); assert.equal(mapped.reviewCount, undefined); assert.equal(mapped.soldCount, undefined);
  assert.equal(mapped.isMall, undefined); assert.equal(mapped.rating, 0);
  assert.equal(mapProduct({ ...product, price: null }).price, undefined);
});

test('variant attributes and original prices come directly from the real SKU', () => {
  const mapped = mapProduct({ ...product, skus: [{ id: 10, skuCode: 'REAL', price: 100, originalPrice: 120, stockQuantity: 0, skuAttributes: '{"Size":"M"}' }] });
  assert.equal(mapped.productSkus[0].id, 10); assert.equal(mapped.originalPrice, 120);
  assert.deepEqual(mapped.productSkus[0].attributes, { Size: 'M' });
  assert.equal(mapped.productSkus[0].stockQuantity, 0);
});

test('category mapper preserves children at arbitrary depth', () => {
  const category = mapCategory({ id: 1, children: [{ id: 2, children: [{ id: 3, children: [{ id: 4 }] }] }] });
  assert.equal(category.children[0].children[0].children[0].id, 4);
});

test('search sends server filters and one-based page beyond the first fifty results', async () => {
  transport = async config => {
    assert.equal(config.url, '/products'); assert.equal(config.params.page, 4); assert.equal(config.params.keyword, 'needle');
    assert.equal(config.params.categoryId, 3); assert.equal(config.params.minPrice, 100); assert.equal(config.params.sortDirection, 'ASC');
    return { config, status: 200, statusText: 'OK', headers: {}, data: { data: { page: 4, size: 20, totalElements: 80, totalPages: 4, items: [product] } } };
  };
  const page = await catalogService.search({ page: 4, size: 20, keyword: 'needle', categoryId: 3, minPrice: 100, sortBy: 'price', sortDirection: 'ASC' });
  assert.equal(page.totalElements, 80); assert.equal(page.page, 4);
});
